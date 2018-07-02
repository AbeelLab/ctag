package nl.tudelft.pl2.representation.graph.handles;

import javafx.util.Pair;
import nl.tudelft.pl2.data.caches.MasterCache;
import nl.tudelft.pl2.data.gff.TraitMap;
import nl.tudelft.pl2.data.storage.HeatMap;
import nl.tudelft.pl2.representation.GraphBuilderHelper;
import nl.tudelft.pl2.representation.GraphPosition;
import nl.tudelft.pl2.representation.exceptions.NodeNotFoundException;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import nl.tudelft.pl2.representation.graph.ZoomDirection;
import nl.tudelft.pl2.representation.ui.InfoSidePanel.SampleSelectionController;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;


/**
 * Class representing an entire graph, a full GFA file, in main memory.
 *
 * Since X ⊆ X a full graph is also a partial graph.
 * This means that one should be able to use a full graph
 * as a substitute for a partial graph and as such it
 * extends the PartialGraph class.
 *
 * The GraphHandle class is used to get required parts of
 * the Graph in the GFA file when they are needed. Because the
 * Full Graph class already has the entire Graph loaded it is not
 * needed to have a separate Graph Handler to manage this.
 * Because of this the class extends the GraphHandle itself
 */
@SuppressWarnings("checkstyle:methodcount")
public class FullGraph extends Observable
        implements GraphHandle {

    /**
     * The {@link GraphBuilderHelper} used to build this
     * {@link FullGraph}.
     */
    private GraphBuilderHelper helper;

    /**
     * Map that maps a segment id to its vertical index.
     */
    private Map<Integer, Integer> verticalMap;

    /**
     * Map that stores a set of Segments based on layer.
     */
    private Map<Integer, HashSet<Node>> layerMap;

    /**
     * The current focused centre node.
     */
    private Node centre;

    /**
     * The current position of the graph.
     */
    private GraphPosition currentPosition;

    /**
     * Map that maps the tag of a header to its content.
     */
    private Map<String, String> headers;

    /**
     * List that keeps track of the observers of the graphHandle.
     */
    private List<Observer> observers;

    /**
     * Boolean to show if the graph has been loaded or not.
     */
    private boolean loaded;

    /**
     * The amount of layers shown in the graph.
     */
    private int shownLayers;

    /**
     * The trait map in the graph.
     */
    private TraitMap tMap;

    /**
     * The cache in the handle.
     */
    private MasterCache cache;

    /**
     * Constructor for a full graph representation in memory.
     *
     * @param layerMapIn    Map mapping layers (Integers) to Segments.
     * @param verticalMapIn The vertical layer assignment map to use.
     * @param helperIn      Builder helper used in the construction
     *                      of the graph.
     */
    public FullGraph(final Map<Integer, HashSet<Node>> layerMapIn,
                     final Map<Integer, Integer> verticalMapIn,
                     final GraphBuilderHelper helperIn) {
        this.helper = helperIn;
        this.verticalMap = verticalMapIn;
        this.layerMap = layerMapIn;
        this.currentPosition = new GraphPosition(0, 1);
        this.observers = new LinkedList<>();
        updateCentre();
    }

    /**
     * Get the segments in a specific layer.
     *
     * @param layer The layer to get.
     * @return The list of segments in the layer
     */
    @Override
    public final Set<Node> getSegmentsFromLayer(final int layer) {
        assert layerMap != null;
        if (!layerMap.containsKey(layer)) {
            return new HashSet<>();
        }
        return layerMap.get(layer);
    }

    @Override
    public final Node centre() {
        return centre;
    }

    @Override
    public final Pair<MoveDirection, Integer> setCentre(final Node node) {
        Pair<MoveDirection, Integer> movePair =
                setCentre(node, currentPosition);
        this.centre = node;
        return movePair;
    }

    @Override
    public final synchronized void updateHeaders(
            final Map<String, String> newHeaders) {

        this.headers = newHeaders;
        for (Observer observer : observers) {
            observer.update(this, updateGenomes());
        }
    }

    @Override
    public final int getSegmentLayer(final int id) {
        return helper.getNodeByID(id).layer();
    }

    @Override
    public final void updateShownLayers(final double layers) {
        this.shownLayers = (int) Math.ceil(layers);
    }

    /**
     * Update the genomes in the graph using the options map.
     *
     * In this method the option map is used to determine the
     * genome samples that are in the graph. It determines this
     * using the static tag definition in GenomeMenuController.
     *
     * Because the fullGraph object is created before all of the headers
     * are encountered more genome names can be encountered during parsing.
     *
     * Because loading the graph is a concurrent process
     * the method is synchronized.
     *
     * @return The set with the genome strings in the graph
     */
    private synchronized String[] updateGenomes() {
        if (headers
                .containsKey(SampleSelectionController.GENOME_TAG)) {
            return headers.get(SampleSelectionController.GENOME_TAG)
                    .split(";");
        } else {
            return new String[0];
        }
    }

    @Override
    public final void updateCentre() {
        Iterator<Node> iterator = getSegmentsFromLayer(
                currentPosition.layer()
        ).iterator();
        if (!iterator.hasNext()) {
            return;
        }
        this.centre = iterator.next();
    }

    @Override
    public final int move(final int steps, final MoveDirection direction) {
        GraphPosition previousPosition = position();
        GraphPosition nextPosition =
                move(
                        steps,
                        direction,
                        previousPosition,
                        0,
                        getMaxLayer());
        moveToLayer(nextPosition.layer());
        return Math.abs(previousPosition.layer() - nextPosition.layer());
    }

    @Override
    public final int moveToLayer(final int layer) {
        int nextLayer = Math.max(0, Math.min(getMaxLayer(), layer));
        currentPosition = new GraphPosition(nextLayer, currentPosition.zoom());
        updateCentre();
        return nextLayer;
    }

    @Override
    public final int semanticZoom(
            final int steps,
            final ZoomDirection direction) {
        int zoom = currentPosition.zoom();
        int newZoom = direction == ZoomDirection.IN
                ? zoom - steps : zoom + steps;
        return Math.abs(newZoom - zoom);
    }

    @Override
    public final GraphPosition position() {
        return currentPosition;
    }

    @Override
    public final void unload() {
        if (shownLayers != 0) {
            shownLayers = 0;
        }
        layerMap.clear();
        helper = null;
    }

    @Override
    public final int getMaxRow() {
        return verticalMap.values().stream()
                .max(Comparator.naturalOrder())
                .orElse(0);
    }

    @Override
    public final int getMaxLayer() {
        return helper.maxLayer();
    }

    @Override
    public final int getCentreLayer() {
        return this.currentPosition.layer();
    }

    @Override
    public final int getVerticalPosition(final Node node) {
        if (verticalMap.containsKey(node.id())) {
            return verticalMap.get(node.id());
        }
        throw new NodeNotFoundException("Node with id : " + node.id()
                + " was not found in vertical map", this.getClass().getName());
    }

    @Override
    public final Set<Integer> getLayerSet() {
        return new HashSet<>(layerMap.keySet());
    }

    @Override
    public final void registerObserver(final Observer observer) {
        observers.add(observer);
    }

    @Override
    public final int genomeCount() {
        return updateGenomes().length;
    }

    @Override
    public final String getGraphName() {
        return "";
    }


    @Override
    public final TraitMap getTraitMap() {
        return tMap;
    }

    @Override
    public final Set<Node> getNodesByGenome(final String genome) {
        return new HashSet<>();
    }

    @Override
    public final HeatMap getHeatMap() {
        return null;
    }

    @Override
    public final void setTraitMap(final TraitMap traitMap) {
        this.tMap = traitMap;
    }

    @Override
    public final String[] getGenomes() {
        return updateGenomes();
    }

    @Override
    public final void setCache(final MasterCache masterCache) {
        this.cache = masterCache;
    }

    @Override
    public final MasterCache retrieveCache() {
        return cache;
    }


    @Override
    public final synchronized void signalLoaded(final LoadingState state) {
        this.updateCentre();
        this.loaded = true;
        for (Observer observer : observers) {
            observer.update(this, LoadingState.FULLY_LOADED);
        }
    }

    @Override
    public final boolean isLoaded() {
        return loaded;
    }
}
