package nl.tudelft.pl2.representation.ui.graph;

import nl.tudelft.pl2.representation.exceptions.NodeNotFoundException;
import nl.tudelft.pl2.representation.external.Bubble;
import nl.tudelft.pl2.representation.external.Chain;
import nl.tudelft.pl2.representation.external.Edge;
import nl.tudelft.pl2.representation.external.Indel;
import nl.tudelft.pl2.representation.external.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Class accompanying {@link NodeDrawer} to complete setting
 * up the graph and updating it when updates are required to
 * happen.
 *
 * @author Chris Lemaire
 */
public class DrawerUpdater {

    /**
     * The {@link NodeDrawerData} object enclosing data
     * used by node-drawing procedures.
     */
    private NodeDrawerData data;

    /**
     * The genome painter for the drawer.
     */
    private GenomePainter genomePainter;

    /**
     * Constructs a new DrawerUpdater with the {@link NodeDrawerData}
     * object as is also observed by a coupled {@link NodeDrawer}.
     *
     * @param dataIn The {@link NodeDrawerData} object.
     */
    DrawerUpdater(final NodeDrawerData dataIn) {
        this.data = dataIn;
    }

    /**
     * Sets up the graph after zooming in/out around the given position.
     *
     * @param x The x-coordinate around which the graph is zoomed.
     * @param y The y-coordinate around which the graph is zoomed.
     */
    final void setupGraphAfterZoom(final double x, final double y) {
        data.coordinates().recalculateZoom(data.shownLayers().get(), x, y);
        setupGraph();
    }

    /**
     * This method creates the new gui representation
     * from the graph. It recreates the nodesByLayerMap
     * and nodesByNameMap objects with current
     * state of the graph object.
     */
    final void setupGraph() {
        // Cannot perform while graph is not loaded.
        if (data.graph() == null) {
            return;
        }

        // Recalculate the left-most layer visible on-screen.
        data.coordinates().recalculateLayers(data.shownLayers().get());

        // Calculate starting layer based on centre node.
        int lowerBound = data.coordinates().lowerBound();
        int upperBound = data.coordinates().upperBound();

        // Calculate border layers, the left-most and right-most
        // layers that are and will still be loaded.
        int leftBorder = Integer.MIN_VALUE;
        int rightBorder = Integer.MIN_VALUE;

        if (!data.nodesByLayerMap().isEmpty()) {
            leftBorder = Math.max(
                    data.layersLoaded().first(), lowerBound);
            rightBorder = Math.min(
                    data.layersLoaded().last(), upperBound);
        }

        this.data.drawableTraitTreeMap().clear();
        // Decide on whether to smart-reload or just start over.
        if (rightBorder - leftBorder <= 1) {
            fullReload(lowerBound, upperBound);
        } else {
            dynamicReload(lowerBound, upperBound);
        }
    }

    /**
     * Reloads the graph in a dynamic way. This dynamic reloading
     * removes layers that are no longer needed for drawing and
     * only adds layers that were not loaded yet.
     *
     * @param lowerBound The lower-bound layer to have loaded.
     * @param upperBound The upper-bound layer to have loaded.
     */
    private void dynamicReload(final int lowerBound,
                               final int upperBound) {
        // Find layers that are to be removed.
        Set<Integer> replacedLayers = new HashSet<>();
        for (Integer layer : data.nodesByLayerMap().keySet()) {
            if (layer < lowerBound || layer > upperBound) {
                replacedLayers.add(layer);
            }
        }

        // Clear layers of nodes that are not in the current
        // shown layer range from nodesByLayerMap and nodesByNameMap.
        for (Integer layer : replacedLayers) {
            data.nodesByLayerMap().get(layer).values().forEach(n ->
                    data.nodesByNameMap().remove(n.getNode().id()));
            data.nodesByLayerMap().remove(layer);
        }

        // Setup layers that need setting up.
        for (int i = lowerBound; i <= upperBound; i++) {
            if (!data.nodesByLayerMap().containsKey(i)) {

                setupLayer(i);
            }
        }
    }

    /**
     * Reloads the graph in full. This way of reloading the
     * entire list of {@link DrawableNode}s is cleared and
     * the layers between lowerBound and upperBound are added.
     *
     * @param lowerBound The lower-bound layer to have loaded.
     * @param upperBound The upper-bound layer to have loaded.
     */
    private void fullReload(final int lowerBound,
                            final int upperBound) {

        data.nodesByNameMap().clear();
        data.nodesByLayerMap().clear();

        for (int i = lowerBound; i <= upperBound; i++) {
            setupLayer(i);
        }
    }

    /**
     * Sets up a single layer in the nodesByNameMap and
     * nodesByLayerMap, converting {@link Node}s to
     * {@link DrawableNode}s.
     *
     * @param actualLayer The layer to be added to the
     *                    shown graph.
     */
    private void setupLayer(final int actualLayer) {
        Set<Node> graphNodes = data.graph().getSegmentsFromLayer(actualLayer);
        Iterator<Node> iterator = graphNodes.iterator();
        HashMap<Integer, DrawableAbstractNode> newNodes = new HashMap<>();

        iterator.forEachRemaining((seg) -> {
            int yPos;
            try {
                yPos = data.graph().getVerticalPosition(seg);
            } catch (NodeNotFoundException e) {
                yPos = 0;
            }

            DrawableAbstractNode newNode
                    = convertToNode(seg, actualLayer, yPos);
            newNodes.put(yPos, newNode);
            data.nodesByNameMap().put(seg.id(), newNode);
        });

        if (!newNodes.isEmpty()) {
            data.nodesByLayerMap().put(actualLayer, newNodes);
            data.layersLoaded().add(actualLayer);
        }
    }

    /**
     * Converts a segment to a node which can be understood
     * by GUI representation.
     *
     * @param node  A segment which is pulled from the backend.
     * @param layer The layer in which the segment is present.
     * @param row   The y coordinate for the start of the
     *              node.
     * @return A node which can be understood by the GUI.
     */
    private DrawableAbstractNode convertToNode(final Node node,
                                               final int layer,
                                               final int row) {
        NodeData nodeData = new NodeData(node, layer,
                row, data.coordinates(), data.nodesByNameMap());

        if (node.incoming().iterator().hasNext()) {

            Edge link = node.incoming().iterator().next();

            Iterator<Node> iterator
                    = data.graph().getSegmentsFromLayer(layer - 1).iterator();
            Node newNode = null;
            while (iterator.hasNext()) {
                Node next = iterator.next();
                if (next.id() == link.from()) {
                    newNode = next;
                    break;
                }
            }

            if (newNode instanceof Bubble) {
                Bubble bubble = (Bubble) newNode;
                return new DrawableBubble(nodeData, genomePainter, bubble.cHi(),
                        bubble.cLo());
            }

            if (newNode instanceof Indel) {
                Indel indel = (Indel) newNode;
                return new DrawableIndel(nodeData, genomePainter, indel
                        .midContent());
            }

            if (node instanceof Chain) {
                Chain chain = (Chain) newNode;
                return new DrawableChain(nodeData, genomePainter);
            }
        }

        return DrawableNode.from(nodeData, genomePainter,
                data.drawableTraitTreeMap());
    }

    /**
     * Set the painter of the nodeDrawer.
     *
     * @param painter The painter of the graph that this drawer belongs to
     */
    final void setPainter(final GenomePainter painter) {
        this.genomePainter = painter;
    }

    /**
     * Calls the add color to node function of genome painter.
     *
     * @param node Node
     * @return If a color was assigned
     */
    public final boolean addColorToNode(final Node node) {
        return genomePainter != null && genomePainter.addNewNode(node);
    }

    /**
     * Calls the remove color from node function of genome painter.
     *
     * @param node Node
     * @return If a node was removed
     */
    public final boolean removeColorFromNode(final Node node) {
        return genomePainter != null && genomePainter.removeNode(node);
    }

}
