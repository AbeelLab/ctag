package nl.tudelft.pl2.representation.graph.helpers;

import nl.tudelft.pl2.representation.GraphBuilderHelper;
import nl.tudelft.pl2.representation.external.Edge;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.external.components.DummyLink;
import nl.tudelft.pl2.representation.external.components.DummyNode;
import nl.tudelft.pl2.representation.graph.GraphLoader;
import nl.tudelft.pl2.representation.graph.loaders.ChunkedGraphLoader;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class containing the methods
 * needed to add dummy nodes to layers.
 */
public class DummyNodeHelper {
    /**
     * The loader currently used to load a graph.
     */
    private GraphLoader graphLoader;

    /**
     * A map that maps de index of a layer to the segments in said layer.
     */
    private Map<Integer, HashSet<Node>> layerMap;

    /**
     * A helper used in building a graph.
     */
    private GraphBuilderHelper helper;

    /**
     * Lock used for locking the maps.
     */
    private Lock mapLock;

    /**
     * Helper for adding DummyNodes to layers.
     *
     * @param loader             The loader being helped
     * @param map                The layerMap that maps a layer
     *                           index to the segments in the layer
     * @param graphBuilderHelper A helper used in building graphs
     */
    public DummyNodeHelper(final GraphLoader loader,
                           final Map<Integer, HashSet<Node>> map,
                           final GraphBuilderHelper graphBuilderHelper) {
        graphLoader = loader;
        helper = graphBuilderHelper;
        if (loader instanceof ChunkedGraphLoader) {
            mapLock = ((ChunkedGraphLoader) loader).getMapLock();
            try {
                mapLock.lock();
                layerMap = ((ChunkedGraphLoader) loader).getLayerMap();
            } finally {
                mapLock.unlock();
            }
        } else {
            mapLock = new ReentrantLock();
            layerMap = map;
        }
    }

    /**
     * Check all the layers in between the supplied
     * min and max layer and add dummy nodes if needed.
     *
     * @param minLayer The minimum layer to check
     * @param maxLayer The maximum layer to check
     * @return The dummies added
     */
    public final synchronized HashSet<DummyNode> addDummyNodeToLayers(
            final int minLayer,
            final int maxLayer) {
        HashSet<DummyNode> dummyNodes = new HashSet<>();
        try {
            mapLock.lock();
            for (int i = minLayer; i <= maxLayer; i++) {
                if (!layerMap.containsKey(i)) {
                    layerMap.put(i, new HashSet<>());
                }
                HashSet<Node> segmentSet = layerMap.get(i);
                for (Node node : segmentSet) {
                    if (!node.isDummy()) {
                        dummyNodes.addAll(
                                addDummyNodesFromNode(node)
                        );

                    }
                }
            }
            return dummyNodes;
        } finally {
            mapLock.unlock();
        }
    }

    /**
     * Check all of the links in the given segment to see if
     * any DummyNodes should be added in the layers the link spans.
     *
     * @param node The segment to add dummy segments from
     * @return The dummies added
     */
    private HashSet<DummyNode> addDummyNodesFromNode(
            final Node node) {
        HashSet<DummyNode> dummyNodes = new HashSet<>();
        node.outgoing().foreach((edge) -> {
            int fromId = edge.from();
            int toId = edge.to();
            if (!helper.hasNode(fromId) || !helper.hasNode(toId)) {
                return null;
            }
            Node from = helper.getNodeByID(fromId);
            Node toNode = helper.getNodeByID(toId);

            assert from.layer() == node.layer();

            int fromLayer = node.layer();

            int toLayer = toNode.layer();
            assert toLayer > fromLayer;

            if (toLayer - fromLayer > 1) {
                helper.removeOutgoing(node, edge);
                helper.removeIncoming(toNode, edge);
                dummyNodes.addAll(replaceLinkWithDummyTrail(
                        node,
                        toNode,
                        edge
                ));
            }
            return null;
        });
        return dummyNodes;
    }

    /**
     * If a link spans multiple layers dummy nodes
     * should be added in the traversed layers.
     *
     * DummyLinks should then be added between these Dummy Nodes
     *
     * @param fromNode The Node the link comes from
     * @param toNode   The Node the link goes to
     * @param link     The link to replace
     * @return The dummies added
     */
    private HashSet<DummyNode> replaceLinkWithDummyTrail(
            final Node fromNode,
            final Node toNode,
            final Edge link) {
        HashSet<DummyNode> dummyNodes = new HashSet<>();

        Node previousNode;
        Node nextNode = fromNode;
        DummyLink dummyLink;

        for (int i = fromNode.layer() + 1; i < toNode.layer(); i++) {
            int maxNodeID = graphLoader.createNewId();

            previousNode = nextNode;
            nextNode = new DummyNode(maxNodeID, i);
            dummyNodes.add((DummyNode) nextNode);
            dummyLink = new DummyLink(
                    previousNode.id(), nextNode.id(), link
            );

            helper.addOutgoing(previousNode, dummyLink);
            helper.addIncoming(nextNode, dummyLink);

            try {
                mapLock.lock();
                if (!layerMap.containsKey(i)) {
                    layerMap.put(i, new HashSet<>());
                }


                layerMap.get(i).add(nextNode);
            } finally {
                mapLock.unlock();
            }
        }

        dummyLink = new DummyLink(nextNode.id(), toNode.id(), link);

        helper.addOutgoing(nextNode, dummyLink);
        helper.addIncoming(toNode, dummyLink);
        return dummyNodes;
    }
}
