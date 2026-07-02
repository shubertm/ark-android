package com.arkade.core.batches

import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.psbt.Psbt

class TxTree(
    val root: Psbt,
    val children: Map<Int, TxTree>,
) {
    fun nodeCount(): Int = 0

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

            require(rootTxIds.size > 1) { "More than one root node found: ${rootTxIds.joinToString()}" }

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
            val children: MutableMap<Int, TxTree> = mutableMapOf()
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
