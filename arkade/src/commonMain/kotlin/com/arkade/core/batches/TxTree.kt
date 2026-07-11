package com.arkade.core.batches

import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.psbt.Psbt

class TxTree(
    val root: Psbt,
    val children: Map<Long, TxTree>,
) : Iterable<TxTree> {
    fun nodeCount(): Int =
        1 +
            children.values.sumOf {
                it.nodeCount()
            }

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

    override fun iterator(): Iterator<TxTree> =
        iterator {
            yield(this@TxTree)
            for (child in children.values) {
                yieldAll(child)
            }
        }

    companion object {
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
