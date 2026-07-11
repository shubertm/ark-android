package com.arkade.core.batches

import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.psbt.Psbt

/**
 * A tree of linked transactions, such as the connector or VTXO trees used during batch
 * finalization/signing, modeled as a [root] transaction whose spent outputs are each mapped to
 * the [TxTree] descending from them.
 *
 * @property root The transaction at this node of the tree.
 * @property children Maps each output index of [root] that is spent within the tree to the
 * [TxTree] rooted at the transaction spending it.
 */
class TxTree(
    val root: Psbt,
    val children: Map<Long, TxTree>,
) : Iterable<TxTree> {
    /**
     * Counts the number of nodes in this tree, including this node and all descendants.
     */
    fun nodeCount(): Int =
        1 +
            children.values.sumOf {
                it.nodeCount()
            }

    /**
     * Recursively validates the structural and monetary consistency of this tree.
     *
     * Requires that [root] has exactly one input and that the number of [children] does not
     * exceed the number of spendable outputs (`numberOfOutputs - 1`). For every child, checks
     * that its keyed output index is in bounds, recursively validates the child, verifies that
     * the child's single input spends [root]'s output at that index, and requires that the sum
     * of the child transaction's outputs equals the amount of the parent output it spends.
     *
     * @throws IllegalArgumentException if any of the above conditions are violated.
     */
    fun validate() {
        val tx = root.global.tx
        val numberOfInputs = tx.txIn.size
        val numberOfOutputs = tx.txOut.size
        require(numberOfInputs == 1) { "Unexpected number of inputs $numberOfInputs, expected 1" }
        require(
            children.size <= numberOfOutputs - 1,
        ) { "Unexpected number of children ${children.size}, expected maximum ${numberOfOutputs - 1}" }
        children.forEach { (outputIndex, child) ->
            require(
                outputIndex < numberOfOutputs,
            ) { "Output index $outputIndex is outputIndex of bounds, expected less than $numberOfOutputs" }
            child.validate()
            val childTx = child.root.global.tx
            val childTxInput = childTx.txIn[0]
            val parentTxId = tx.txid
            require(parentTxId == childTxInput.outPoint.txid) {
                "input of child ${childTxInput.outPoint.index} is not the output of the parent"
            }
            require(childTxInput.outPoint.index == outputIndex) {
                "input of child ${childTxInput.outPoint.index} is not the output of the parent"
            }

            val childOutputsTotalAmount = childTx.txOut.sumOf { it.amount.sat }
            val parentOutputAmount = tx.txOut[outputIndex.toInt()].amount.sat
            require(childOutputsTotalAmount == parentOutputAmount) {
                "sum of child's outputs != parent output: $childOutputsTotalAmount != $parentOutputAmount"
            }
        }
    }

    /**
     * Recursively collects the transactions at the leaves of this tree (nodes with no children).
     *
     * @return The leaf transactions, in depth-first order; a single-element list containing
     * [root] if this node itself is a leaf.
     */
    fun leaves(): Collection<Psbt> {
        if (children.isEmpty()) {
            return listOf(root)
        }
        val leaves = mutableListOf<Psbt>()
        children.forEach { (_, child) ->
            leaves.addAll(child.leaves())
        }
        return leaves
    }

    /**
     * Depth-first pre-order iterator over this node and all its descendants.
     */
    override fun iterator(): Iterator<TxTree> =
        iterator {
            yield(this@TxTree)
            for (child in children.values) {
                yieldAll(child)
            }
        }

    companion object {
        /**
         * Assembles a [TxTree] from a flat list of [TxTreeNode]s received from the server.
         *
         * Indexes [treeNodes] by transaction id, then identifies the single node whose txid is
         * never referenced as a child by any other node, and recursively builds the tree from
         * that root via [buildGraph].
         *
         * @param treeNodes The flat list of nodes to assemble into a tree.
         * @return The assembled [TxTree], rooted at the node with no incoming references.
         * @throws IllegalArgumentException if [treeNodes] is empty, if no root or more than one
         * root can be identified, or if the assembled graph does not contain every node in
         * [treeNodes] (indicating a dangling/missing reference).
         */
        fun create(treeNodes: List<TxTreeNode>): TxTree {
            require(treeNodes.isNotEmpty()) { "Tree nodes cannot be empty" }
            val treeNodesByTxId = mutableMapOf<TxId, TxTreeNode>()
            treeNodes.forEach { node ->
                val txId = node.tx.global.tx.txid
                treeNodesByTxId[txId] = node
            }

            val rootTxIds: MutableList<TxId> = mutableListOf()
            treeNodesByTxId.forEach { (txId, _) ->
                if (!treeNodesByTxId.values.any { node -> node.children.containsValue(txId) }) {
                    rootTxIds.add(txId)
                }
            }

            require(rootTxIds.isNotEmpty()) { "No root node found" }

            require(rootTxIds.size == 1) { "More than one root node found: ${rootTxIds.joinToString()}" }

            val graph = buildGraph(rootTxIds[0], treeNodesByTxId)

            requireNotNull(graph) { "Root node not found for root txid: ${rootTxIds[0]}" }

            require(graph.nodeCount() == treeNodes.size) {
                "Number of chunks ${treeNodes.size} != nodes in graph ${graph.nodeCount()})"
            }

            return graph
        }

        /**
         * Recursively builds a [TxTree] rooted at [root] by looking up child transactions in
         * [treeNodesByTxId].
         *
         * @param root The txid of the node to build the (sub)tree for.
         * @param treeNodesByTxId All available nodes, indexed by their transaction id.
         * @return The built [TxTree], or `null` if [root] is not present in [treeNodesByTxId].
         */
        private fun buildGraph(
            root: TxId,
            treeNodesByTxId: Map<TxId, TxTreeNode>,
        ): TxTree? {
            val rootNode =
                treeNodesByTxId.getOrElse(root) {
                    return null
                }
            val children: MutableMap<Long, TxTree> = mutableMapOf()
            rootNode.children.forEach { (out, txId) ->
                val childGraph = buildGraph(txId, treeNodesByTxId)
                if (childGraph != null) {
                    children[out] = childGraph
                }
            }
            return TxTree(rootNode.tx, children)
        }
    }
}
