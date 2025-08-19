package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.DijkstraTree
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.findPathToDestination
import java.util.PriorityQueue
import java.util.Stack

object GraphUtils {
    /**
     * Find the fastest path from [closestNode] to *any* node that matches [condition].
     */
    fun findFastestPath(
        closestNode: GraphNode,
        condition: (GraphNode) -> Boolean,
    ): Pair<Graph, Double>? {
        val distances = findDijkstraDistances(closestNode, condition)
        val entry = distances.lastVisitedNode.takeIf(condition)
        return entry?.let {
            distances.findPathToDestination(it)
        }
    }

    /**
     * Find the fastest path from [closestNode] to *all* nodes that matches [condition].
     */
    fun findFastestPaths(
        graph: Graph,
        closestNode: GraphNode,
        condition: (GraphNode) -> Boolean = { true },
    ): Pair<MutableMap<GraphNode, Graph>, MutableMap<GraphNode, Double>> {
        val paths = mutableMapOf<GraphNode, Graph>()

        val map = mutableMapOf<GraphNode, Double>()
        val distances = findAllShortestDistances(closestNode)
        for (graphNode in graph) {
            if (!condition(graphNode)) continue
            val (path, distance) = distances.findPathToDestination(graphNode)
            paths[graphNode] = path
            map[graphNode] = distance
        }
        return Pair(paths, map)
    }

    /**
     * Find all maximal sub graphs of the given graph which are not connected
     */
    fun findDisjointClusters(graph: Graph): List<Set<GraphNode>> {
        val universe = graph.toMutableSet()
        val allClusters = mutableListOf<Set<GraphNode>>()
        while (universe.isNotEmpty()) {
            val cluster = mutableSetOf<GraphNode>()
            allClusters.add(cluster)
            val queue = Stack<GraphNode>()
            queue.add(universe.first())
            while (queue.isNotEmpty()) {
                val next = queue.pop()
                universe.remove(next)
                cluster.add(next)
                queue.addAll(next.neighbours.keys)
                queue.retainAll(universe)
            }
        }
        return allClusters
    }

    fun findShortestPathAsGraph(start: GraphNode, end: GraphNode): Graph = findShortestPathAsGraphWithDistance(start, end).first

    /**
     * Find a tree of distances to the [start] node using dijkstra's algorithm.
     */
    fun findDijkstraDistances(
        start: GraphNode,
        /**
         * Bail out early before collecting all the distances to all nodes in the graph. This will not collect valid distance data for *all*
         * nodes for which bailout matches, but only the closest one.
         */
        bailout: (GraphNode) -> Boolean,
    ): DijkstraTree {
        val distances = mutableMapOf<GraphNode, Double>()
        val previous = mutableMapOf<GraphNode, GraphNode>()
        val visited = mutableSetOf<GraphNode>()
        val queue = PriorityQueue<GraphNode>(compareBy { distances.getOrDefault(it, Double.MAX_VALUE) })
        var lastVisitedNode: GraphNode = start

        distances[start] = 0.0
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (!current.enabled) {
                visited.add(current)
                continue
            }
            lastVisitedNode = current
            if (bailout(current)) break

            visited.add(current)

            current.neighbours.forEach { (neighbour, weight) ->
                if (neighbour !in visited) {
                    val newDistance = distances.getValue(current) + weight
                    if (newDistance < distances.getOrDefault(neighbour, Double.MAX_VALUE)) {
                        distances[neighbour] = newDistance
                        previous[neighbour] = current
                        queue.add(neighbour)
                    }
                }
            }
        }

        return DijkstraTree(
            start,
            distances,
            previous,
            lastVisitedNode,
        )
    }

    @JvmName("findShortestDistancesOnCurrentIslandWithTargets")
    fun findShortestDistancesOnCurrentIsland(
        target: Collection<GraphNode>,
    ): DijkstraTree = findDijkstraDistances(nearestNodeOnCurrentIsland(), target::contains)

    fun findShortestDistancesOnCurrentIsland(
        target: Collection<LorenzVec>,
    ): DijkstraTree = findDijkstraDistances(nearestNodeOnCurrentIsland()) { target.contains(it.position) }

    fun findAllShortestDistancesOnCurrentIsland(
        bailout: (GraphNode) -> Boolean = { false },
    ): DijkstraTree = findDijkstraDistances(nearestNodeOnCurrentIsland(), bailout)

    fun findAllShortestDistancesOnCurrentIsland(
        start: LorenzVec,
        bailout: (GraphNode) -> Boolean = { false },
    ): DijkstraTree = findDijkstraDistances(nearestNodeOnCurrentIsland(start), bailout)

    fun nearestNodeOnCurrentIsland() = nearestNodeOnCurrentIsland(LocationUtils.playerGraphGridLocation())

    fun nearestNodeOnCurrentIsland(location: LorenzVec): GraphNode {
        val graph = IslandGraphs.currentIslandGraph ?: error("no island found")
        return graph.getNearest(location)
    }

    fun findAllShortestDistances(
        start: GraphNode,
        bailout: (GraphNode) -> Boolean = { false },
    ): DijkstraTree {
        return findDijkstraDistances(start, bailout)
    }

    fun findShortestPathAsGraphWithDistance(start: GraphNode, end: GraphNode): Pair<Graph, Double> {
        val distances = findDijkstraDistances(start) { it == end }
        return distances.findPathToDestination(end)
    }

    fun findShortestPath(start: GraphNode, end: GraphNode): List<LorenzVec> = findShortestPathAsGraph(start, end).toPositionsList()

    fun findShortestDistance(start: GraphNode, end: GraphNode): Double = findShortestPathAsGraphWithDistance(start, end).second

    fun calculatePathLength(path: List<LorenzVec>): Double {
        if (path.size < 2) return 0.0
        val mappedNodes = path.map { nearestNodeOnCurrentIsland(it) }
        return mappedNodes.zipWithNext { a, b -> findShortestDistance(a, b) }.sum()
    }
}
