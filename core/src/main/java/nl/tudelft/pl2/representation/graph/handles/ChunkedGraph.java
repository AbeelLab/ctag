package nl.tudelft.pl2.representation.graph.handles;

import javafx.application.Platform;
import javafx.util.Pair;
import nl.tudelft.pl2.data.caches.MasterCache;
import nl.tudelft.pl2.data.gff.TraitMap;
import nl.tudelft.pl2.data.loaders.MasterCacheLoader;
import nl.tudelft.pl2.data.storage.HeatMap;
import nl.tudelft.pl2.representation.GraphPosition;
import nl.tudelft.pl2.representation.exceptions.NodeNotFoundException;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import nl.tudelft.pl2.representation.graph.ZoomDirection;
import nl.tudelft.pl2.representation.graph.loaders.ChunkedGraphLoader;
import nl.tudelft.pl2.representation.ui.InfoSidePanel.SampleSelectionController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Class that handles loading a graph into memory from a cache.
 */
@SuppressWarnings("checkstyle:methodcount")
public class ChunkedGraph extends Observable implements GraphHandle {
    /**
     * Log4J [[Logger]] used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("ChunkedGraph");

    /**
     * The cache that contains the information about the graph.
     */
    private MasterCache cache;

    /**
     * The node in the centre of the current view.
     */
    private Node centreNode;

    /**
     * If the graph is loaded or not.
     */
    private boolean loaded;

    /**
     * The current position in the graph.
     */
    private GraphPosition currentPosition;

    /**
     * A map that maps the layer index to the nodes in that layer.
     */
    private Map<Integer, HashSet<Node>> layerMap;

    /**
     * A map that maps a node id to the vertical index of that node.
     */
    private Map<Integer, Integer> verticalMap;

    /**
     * The loader used to load chunks.
     */
    private ChunkedGraphLoader loader;

    /**
     * The path to the file the graph is loaded from.
     */
    private Path path;

    /**
     * The headers in the graph.
     */
    private Map<String, String> headers;

    /**
     * The genome samples in the graph.
     */
    private String[] genomes;

    /**
     * The observers of this graphHandle.
     */
    private Set<Observer> observers;

    /**
     * The trait map which is loaded for this graph.
     */
    private TraitMap traitMap;

    /**
     * The maximum vertical index assigned at any time
     * during the existence of this {@link ChunkedGraph}.
     */
    private int maxRow = 0;

    /**
     * Constructor for the ChunkedGraph.
     *
     * @param graphLoader Loader used in loading the graph.
     * @param vMap        Map that maps the id of a segment
     *                    to its vertical index
     * @param lMap        Map that maps the index of a layer
     *                    to the nodes in that layer
     * @param gfaPath     The path to the gfa file
     */
    public ChunkedGraph(final ChunkedGraphLoader graphLoader,
                        final Map<Integer, Integer> vMap,
                        final Map<Integer, HashSet<Node>> lMap,
                        final Path gfaPath) {
        this.loader = graphLoader;
        this.path = gfaPath;
        this.verticalMap = vMap;
        this.layerMap = lMap;
        this.headers = Collections.synchronizedMap(new HashMap<>());
        this.genomes = new String[0];
        this.observers = new HashSet<>();
        this.currentPosition = new GraphPosition(0, 0);
    }

    @Override
    public final Node centre() {
        return centreNode;
    }

    @Override
    public final Pair<MoveDirection, Integer> setCentre(final Node node) {
        Pair<MoveDirection, Integer> movePair =
                setCentre(node, currentPosition);
        this.centreNode = node;
        return movePair;
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
        LOGGER.debug("Moving to layer {}", layer);
        loader.resetPreviousLoaded();
        if (currentPosition.layer() == layer) {
            return layer;
        }
        int newLayer = Math.max(0, Math.min(layer, getMaxLayer()));
        currentPosition = new GraphPosition(newLayer, currentPosition.zoom());
        loader.checkForNewChunks();
        if (centreNode == null
                || centreNode.layer() != currentPosition.layer()) {
            this.updateCentre();
        }
        return 0;
    }

    @Override
    public final int semanticZoom(
            final int steps,
            final ZoomDirection direction) {
        int current = currentPosition.zoom();
        int toZoom = direction == ZoomDirection.IN
                ? current - steps : current + steps;
        int zoomed = current;
        if (toZoom != current) {
            loader.clearAllChunks();
            zoomed = cache.setZoomLevel(toZoom);
            loader.checkForNewChunks();
        }
        currentPosition = new GraphPosition(currentPosition.layer(), zoomed);
        return Math.abs(current - zoomed);
    }

    @Override
    public final GraphPosition position() {
        return currentPosition;
    }

    @Override
    public final void unload() {
        loader.clearAllChunks();
        MasterCacheLoader.unload(cache);
    }

    @Override
    public final int getCentreLayer() {
        return currentPosition.layer();
    }

    @Override
    public final void updateCentre() {
        int layer = currentPosition.layer();
        if (layerMap.containsKey(layer)) {
            Iterator<Node> it = layerMap.get(layer).iterator();
            if (it.hasNext()) {
                centreNode = it.next();
            }
        }
    }

    @Override
    public final Set<Node> getSegmentsFromLayer(final int layer) {
        HashSet<Node> returnSet = new HashSet<>();
        loader.getMapLock().lock();
        assert layerMap != null;
        if (layerMap.containsKey(layer)) {
            returnSet.addAll(layerMap.get(layer));
        }
        loader.getMapLock().unlock();
        return returnSet;
    }

    @Override
    public final int getMaxRow() {
        return maxRow;
    }

    @Override
    public final int getMaxLayer() {
        return cache != null ? cache.getMaxLayer() : 0;
    }

    @Override
    public final int getVerticalPosition(
            final Node node) {
        loader.getMapLock().lock();
        try {
            if (verticalMap.containsKey(node.id())) {
                maxRow = Math.max(maxRow, verticalMap.get(node.id()));
                return verticalMap.get(node.id());
            }
        } finally {
            loader.getMapLock().unlock();
        }
        throw new NodeNotFoundException("Node with id : " + node.id()
                + " was not found in vertical map", this.getClass().getName());
    }

    @Override
    public final void registerObserver(final Observer observer) {
        observers.add(observer);
    }

    @Override
    public final synchronized void updateHeaders(
            final Map<String, String> newHeaders) {

        this.headers = newHeaders;
        LOGGER.debug("Headers updated to {}", newHeaders);
        if (headers
                .containsKey(SampleSelectionController.GENOME_TAG)) {
            this.genomes =
                    headers.get(
                            SampleSelectionController.GENOME_TAG)
                            .split(";");
        }
        for (Observer observer : observers) {
            LOGGER.debug(
                    "Sending genomes with length : {}", genomes.length);
            try {
                Platform.runLater(() -> observer.update(this, genomes));
            } catch (IllegalStateException e) {
                observer.update(this, genomes);
            }
        }
    }

    @Override
    public final void signalLoaded(final LoadingState type) {
        this.loaded = true;
        this.updateCentre();
        LOGGER.info("Graph {} has finished loading", getGraphName());
        for (Observer observer : observers) {
            try {
                Platform.runLater(() -> observer.update(this, type));
                Platform.runLater(() -> observer.update(this, genomes));
            } catch (IllegalStateException e) {
                observer.update(this, type);
                observer.update(this, genomes);
            }
        }
    }

    @Override
    public final Set<Integer> getLayerSet() {
        Lock lock = loader.getMapLock();
        try {
            lock.lock();
            return new HashSet<>(layerMap.keySet());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final int getSegmentLayer(final int id) {
        return cache.retrieveNodeByID(id).layer();
    }

    @Override
    public final void updateShownLayers(final double layers) {
        loader.setShownLayers((int) Math.ceil(layers));
    }

    @Override
    public final int genomeCount() {
        return genomes.length;
    }

    @Override
    public final String getGraphName() {
        return path.getFileName().toString();
    }

    @Override
    public final Set<Node> getNodesByGenome(final String genome) {
        Set<Node> nodeSet = new HashSet<>();

        getLayerSet().stream().filter(layer -> layerMap.containsKey(layer))
                .forEach(layer -> nodeSet.addAll(
                        getNodesByGenomeFromLayer(genome, layer)));

        return nodeSet;
    }

    /**
     * Get all of the nodes that contain the given genome from
     * the layer with the given index.
     *
     * @param genome The genome to get all the nodes of
     * @param layer  The layer to get the nodes from
     * @return The nodes in the given layer that contain the genome
     */
    private Set<Node> getNodesByGenomeFromLayer(final String genome,
                                                final int layer) {
        Set<Node> nodeSet = new HashSet<>();

        layerMap.get(layer).forEach(node -> {
            Map<String, String> options = node.getOptions();
            if (!options.containsKey(SampleSelectionController.GENOME_TAG)) {
                return;
            }

            String[] sampleStrings =
                    options.get(SampleSelectionController.GENOME_TAG)
                            .split(";");
            if (sampleStrings.length > 0
                    && compareSample(sampleStrings, genome)) {
                nodeSet.add(node);
            }
        });

        return nodeSet;
    }


    /**
     * Compare the samples in a node to
     * the sample we are interested in.
     * If the node is part of the sample we
     * return true.
     *
     * @param sampleArray The array of samples in the node
     * @param genome      The genome to check.
     * @return boolean.
     */
    private boolean compareSample(final String[] sampleArray,
                                  final String genome) {

        for (String sample : sampleArray) {
            try {
                int index = Integer.parseInt(sample);
                if (genomes[index].equals(genome)) {
                    return true;
                }
            } catch (NumberFormatException e) {
                if (sample.equals(genome)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final void setCache(final MasterCache masterCache) {
        this.cache = masterCache;
    }

    @Override
    public final String[] getGenomes() {
        return genomes.clone();
    }

    @Override
    public final MasterCache retrieveCache() {
        return cache;
    }

    @Override
    public final boolean isLoaded() {
        return loaded;
    }

    @Override
    public final HeatMap getHeatMap() {
        assert this.cache != null;
        return this.cache.heatMap();
    }

    @Override
    public final TraitMap getTraitMap() {
        if (this.traitMap == null) {
            this.traitMap = new TraitMap();
        }
        return this.traitMap;
    }

    @Override
    public final void setTraitMap(final TraitMap map) {
        this.traitMap = map;
        this.signalLoaded(LoadingState.FULLY_LOADED_GFF);
    }
}
