package nl.tudelft.pl2.representation.graph;

import javafx.util.Pair;
import nl.tudelft.pl2.data.caches.MasterCache;
import nl.tudelft.pl2.data.gff.TraitMap;
import nl.tudelft.pl2.data.storage.HeatMap;
import nl.tudelft.pl2.representation.GraphPosition;
import nl.tudelft.pl2.representation.external.Node;

import java.util.Map;
import java.util.Observer;
import java.util.Set;


/**
 * A handle to represent a Graph as it is in memory
 * or on disk. This handle offers the interface between
 * view and model by presenting the operations to move
 * over the graph that are relevant to the view. These
 * operations include {@link #move(int, MoveDirection)}
 * and {@link #semanticZoom(int, ZoomDirection)}.
 *
 * A centre of the graph should be maintained by the
 * implementations of this interface and should be
 * presentable as a Node. Tasks involving fetching
 * information about the graph is off-loaded to the
 * Node interface. This means that the view is required
 * to fetch the centre Node and fetch/query the graph
 * at that point from that Node.
 */
@SuppressWarnings("checkstyle:methodcount")
public interface GraphHandle {
    /**
     * Returns the Chunk that is defined
     * as the centre of the Graph at the
     * current time.
     *
     * @return Chunk that defines the current
     * centre of the graph.
     */
    Node centre();

    /**
     * Set the current center of the graph to
     * the supplied node.
     *
     * @param node The new centre of the graph
     * @return The number of steps taken
     * to arrive at that node and the direction the
     * steps were taken in
     */
    Pair<MoveDirection, Integer> setCentre(Node node);

    /**
     * Moves the centre of the graph to
     * the given moveDirection by the given number of steps.
     *
     * @param steps     The number of steps the centre
     *                  is asked to move.
     * @param direction The direction in which the
     *                  centre of the graph is required
     *                  to move.
     * @return How many steps the centre actually
     * moved. It might happen that the graph
     * moved less than the required number of steps.
     */
    int move(int steps, MoveDirection direction);

    /**
     * Move the centre of the graph to the given layer
     * if it is present in the graph.
     *
     * @param layer The layer to move to
     * @return The layer actually moved to. It might happen that the layer
     * you are trying to move to is not in the graph. It will then move
     * to the closest layer in the graph.
     */
    int moveToLayer(int layer);

    /**
     * Zooms the graph in the given direction
     * (in/out) for the given number of steps
     * relative to the current centre.
     *
     * @param steps     The number of steps to
     *                  zoom.
     * @param direction The direction in which the
     *                  graph is required to zoom
     *                  (currently always in/out).
     * @return How many steps the graph was actually
     * zoomed. It might happen that the graph
     * zoomed less than the required number of steps.
     */
    int semanticZoom(int steps, ZoomDirection direction);

    /**
     * Sets the semantic zoom level to the given zoom value.
     *
     * @param zoom The zoom level to set semantic zoom to.
     */
    default void setSemanticZoom(final int zoom) {
        int diff = zoom - position().zoom();
        if (diff < 0) {
            semanticZoom(-diff, ZoomDirection.IN);
        } else {
            semanticZoom(diff, ZoomDirection.OUT);
        }
    }

    /**
     * Returns a GraphPosition object representing
     * the current state of the view on the Graph.
     * This includes the zoom level and some
     * coordinates.
     *
     * @return A GraphPosition object.
     */
    GraphPosition position();

    /**
     * Unloads the graph from memory,
     * cleaning up any memory space used
     * by the graph and its indexing structure.
     */
    void unload();

    /**
     * Get the currently focused central layer in the Graph.
     *
     * @return The current layer
     */
    int getCentreLayer();

    /**
     * Set the centre node to a node present in the current layer.
     */
    void updateCentre();

    /**
     * Get the segments in a specific layer.
     *
     * @param layer The layer to get.
     * @return The list of segments in the layer
     */
    Set<Node> getSegmentsFromLayer(int layer);

    /**
     * Getters for the maximum encountered row (vertical index).
     *
     * @return The maximum row encountered.
     */
    int getMaxRow();

    /**
     * Getter for max layer of the Graph.
     *
     * @return The max layer
     */
    int getMaxLayer();

    /**
     * Get the vertical position of the given node.
     *
     * @param node The node to get the vertical position of
     * @return The vertical position
     */
    int getVerticalPosition(Node node);

    /**
     * Register an observer to observe the GraphHandle.
     *
     * @param observer The observer to register
     */
    void registerObserver(Observer observer);

    /**
     * Update the headers in the graph to the given set.
     *
     * @param headers The new headerMap
     */
    void updateHeaders(Map<String, String> headers);

    /**
     * Signal the graph that it is fully loaded.
     *
     * This method should be called from the GraphLoader when it
     * has finished loading the GraphHandle.
     *
     * @param type The type of loading statye which is being signalled.
     */
    void signalLoaded(LoadingState type);

    /**
     * Checks whether the graph has been loaded or not.
     *
     * @return Whether the graph has been loaded
     */
    boolean isLoaded();

    /**
     * Get the layer of a certain segment.
     *
     * @param id The id of the segment from which the
     *           segment should be returned.
     * @return The layer of the segment with
     * specified id.
     */
    int getSegmentLayer(int id);

    /**
     * Get the set of layers currently in the graph.
     *
     * @return The layers currently loaded.
     */
    Set<Integer> getLayerSet();

    /**
     * Update the shownLayers in the GraphHandle.
     *
     * @param shownLayers The number of layers shown in the current view
     */
    void updateShownLayers(double shownLayers);

    /**
     * Return the number of genomes in the graph.
     *
     * @return The number of genome samples in the graph
     */
    int genomeCount();


    /**
     * Default method for moving in a graph.
     *
     * @param steps           The number of steps to move
     * @param direction       The direction to move in
     * @param currentPosition The current position in the graph
     * @param minLayer        The lowest layer in the current view
     * @param maxLayer        The highest layer in the current view
     * @return The new position in the graph
     */
    default GraphPosition move(int steps,
                               MoveDirection direction,
                               GraphPosition currentPosition,
                               int minLayer,
                               int maxLayer) {
        int layer = currentPosition.layer();
        switch (direction) {
            case LEFT:
                layer -= steps;
                if (layer < minLayer) {
                    layer = minLayer;
                }
                break;
            case RIGHT:
                layer += steps;
                if (layer > maxLayer) {
                    layer = maxLayer;
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid direction : " + direction
                );
        }
        return new GraphPosition(
                layer,
                currentPosition.zoom());
    }

    /**
     * Set the current center of the graph to
     * the supplied node.
     *
     * @param node            The new centre of the graph
     * @param currentPosition The current position of the graphHandle
     * @return The amount of steps taken
     * to arrive at that node and the direction the
     * steps were taken in
     */
    default Pair<MoveDirection, Integer> setCentre(
            final Node node,
            final GraphPosition currentPosition) {
        int toMove = node.layer() - currentPosition.layer();
        MoveDirection direction;
        if (toMove >= 0) {
            direction = MoveDirection.RIGHT;
        } else {
            direction = MoveDirection.LEFT;
        }
        int steps = move(Math.abs(toMove), direction);
        return new Pair<>(direction, steps);
    }

    /**
     * Update the bounds in the graph handle using the coordinate system.
     *
     * @param lowerBound The lower bound of the screen
     * @param upperBound The upper bound of the screen
     */
    default void updateBounds(
            final int lowerBound,
            final int upperBound) {
        if (!isLoaded()) {
            return;
        }
        int newCentre = lowerBound + ((upperBound - lowerBound) / 2);
        updateShownLayers(upperBound - lowerBound);
        moveToLayer(newCentre);
    }

    /**
     * Get the nodes that are part of a specific genome.
     *
     * @param genome genome name.
     * @return Set of nodes.
     */
    Set<Node> getNodesByGenome(String genome);

    /**
     * Gets the heatmap which is associated with the graph.
     *
     * @return An instance of {@link HeatMap} of the graph.
     */
    HeatMap getHeatMap();

    /**
     * Gets the maximum amount of layers a graph has.
     *
     * @return The integer value of the maximum number
     * of layers.
     */
    String getGraphName();

    /**
     * Getter for the trait map used by the graph.
     *
     * @return The trait map which was assigned to this class
     * or a new one if there wasn't any assigned.
     */
    TraitMap getTraitMap();

    /**
     * Add a trait map to the graph.
     *
     * @param map An instance of trait map.
     */
    void setTraitMap(TraitMap map);

    /**
     * @return The list of genome names.
     */
    String[] getGenomes();

    /**
     * Set the Cache of the ChunkedGraph to the given Cache.
     *
     * @param masterCache The new cache
     */
    void setCache(MasterCache masterCache);

    /**
     * Retrieve the cache from the handle.
     *
     * @return The cache in the handle
     */
    MasterCache retrieveCache();
}
