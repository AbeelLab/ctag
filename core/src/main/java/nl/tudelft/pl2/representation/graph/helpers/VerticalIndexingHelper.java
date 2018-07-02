package nl.tudelft.pl2.representation.graph.helpers;

import nl.tudelft.pl2.representation.external.Edge;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphLoader;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import nl.tudelft.pl2.representation.graph.loaders.ChunkedGraphLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.collection.Iterator;
import scala.collection.mutable.Buffer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that contains the methods needed
 * for assigning vertical indexes to a graph.
 */
public class VerticalIndexingHelper {
    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("VerticalIndexingHelper");

    /**
     * Map that maps the index of a
     * layer to the segments in that layer.
     */
    private Map<Integer, HashSet<Node>> layerMap;

    /**
     * Map that maps the id of a segment to its vertical index.
     */
    private Map<Integer, Integer> verticalMap;

    /**
     * The highest vertical index in the next layer.
     */
    private int nextLayerMax = 0;

    /**
     * The lock used to reserve the map for the current thread.
     */
    private Lock mapLock;

    /**
     * Helper for assigning vertical indexes to segments.
     *
     * @param lMap   Map that maps a layer index to
     *               the segments in that layer
     * @param vMap   Map that maps a segment ID
     *               to its vertical index
     * @param loader The loader that the helper is used in
     */
    public VerticalIndexingHelper(
            final Map<Integer, HashSet<Node>> lMap,
            final Map<Integer, Integer> vMap,
            final GraphLoader loader) {
        if (loader instanceof ChunkedGraphLoader) {
            ChunkedGraphLoader chunkedLoader = (ChunkedGraphLoader) loader;
            this.mapLock = chunkedLoader.getMapLock();
            try {
                this.mapLock.lock();
                this.layerMap = chunkedLoader.getLayerMap();
            } finally {
                this.mapLock.unlock();
            }
        } else {
            this.layerMap = lMap;
            this.mapLock = new ReentrantLock();
        }
        this.verticalMap = vMap;
    }

    /**
     * Calculate the index of a segment
     * based on the verticals of its neighbours.
     *
     * @param verticals   The vertical indexes of the neighbours
     * @param sum         The sum of the neighbouring indexes
     * @param verticalSet The set of already assigned
     *                    indexes in the current layer
     * @return The index of the segment
     */
    private int calculateVerticalIndex(
            final int[] verticals,
            final double sum,
            final HashSet<Integer> verticalSet) {
        Arrays.sort(verticals);
        double average = verticals.length != 0
                ? sum / (double) verticals.length : 0;
        int middle = verticals.length / 2;
        int median;
        if (verticals.length % 2 == 1) {
            median = verticals[middle];
        } else if (verticals.length > 0) {
            median = (verticals[middle] + verticals[middle - 1]) / 2;
        } else {
            median = 0;
        }
        int index = median;
        int step = average >= median ? 1 : -1;
        while (verticalSet.contains(index) || index < 0) {
            index += step;
            if (index < 0 && step == -1) {
                step = 1;
            }
        }
        return index;
    }

    /**
     * Add the vertical orientation to
     * all of the segments in the graph.
     *
     * @param from The layer to start assigning from
     * @param to   The layer to work towards
     */
    public final synchronized void addVerticalOrientation(
            final int from,
            final int to) {
        LOGGER.debug("Adding vertical indexes");
        try {
            mapLock.lock();
            java.util.Iterator<Node> iterator =
                    layerMap.get(from).iterator();
            Node segment;
            for (int i = 0; iterator.hasNext(); i++) {
                segment = iterator.next();
                if (verticalMap.containsKey(segment.id())) {
                    continue;
                }
                verticalMap.put(segment.id(), i);
            }
            if (from < to) {
                for (int i = from; i <= to; i++) {
                    assert layerMap.containsKey(i);
                    assignVerticalToLayer(
                            layerMap.get(i), MoveDirection.RIGHT);
                }
            } else {
                for (int i = from - 1; i >= to; i--) {
                    assert layerMap.containsKey(i);
                    assignVerticalToLayer(
                            layerMap.get(i), MoveDirection.LEFT);
                }
            }
        } finally {
            mapLock.unlock();
        }
    }

    /**
     * Assign vertical indexes to the segments of a layer.
     *
     * @param segments        The segments in the layer
     * @param assignDirection The direction to assign the indexes in
     */
    private void assignVerticalToLayer(
            final HashSet<Node> segments,
            final MoveDirection assignDirection) {
        HashSet<Integer> verticalSet = new HashSet<>();
        this.nextLayerMax = 0;
        for (Node segment : segments) {
            Buffer<Edge> edges;
            if (assignDirection.equals(MoveDirection.LEFT)) {
                edges = segment.outgoing();
            } else {
                edges = segment.incoming();
            }
            int[] verticals = new int[edges.length()];
            double sum = 0.0;
            Iterator<Edge> iterator = edges.iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                Edge link = iterator.next();
                int id;
                if (assignDirection.equals(MoveDirection.LEFT)) {
                    id = link.to() == segment.id()
                            ? link.to() : link.from();
                } else {
                    id = link.to() == segment.id()
                            ? link.from() : link.to();
                }

                if (!verticalMap.containsKey(id)) {
                    addOutOfScopeVertical(id, i);
                }
                assert verticalMap.containsKey(id);
                verticals[i] = verticalMap.get(id);
                sum += (double) verticals[i];
            }

            int index = calculateVerticalIndex(verticals, sum, verticalSet);

            verticalMap.put(segment.id(), index);
            verticalSet.add(index);
        }

        // The lines below shift the parts that can be moved upwards
        // depending on the space left above the highest vertical index.

        PriorityQueue<Node> nodeQueue = new PriorityQueue<>(
                Comparator.comparingInt(o -> verticalMap.get(o.id()))
        );

        nodeQueue.addAll(segments);

        for (int i = 0; !nodeQueue.isEmpty(); i++) {
            Node node = nodeQueue.poll();
            verticalMap.put(node.id(), i);
        }

        // This code could be readded in the future and as such I left it here

//        for (Node segment : segments) {
//            int old = verticalMap.get(segment.id());
//            if (old - min < 0) {
//                System.out.println("INDEX : " + (old - min));
//            }
//            verticalMap.put(segment.id(), old - min);
//        }
    }

    /**
     * Add an index to a segment
     * that points to a segment that has not been loaded.
     *
     * @param id    The id of the segment that point out of the scope
     * @param layer The next layer relative to the segment
     */
    private void addOutOfScopeVertical(
            final int id,
            final int layer) {
        if (layerMap.containsKey(layer + 1)) {
            for (Node s : layerMap.get(layer + 1)) {
                if (!verticalMap.containsKey(s.id())) {
                    continue;
                }
                nextLayerMax =
                        Math.max(nextLayerMax, verticalMap.get(s.id()));
            }
        }
        nextLayerMax += 1;
        verticalMap.put(id, nextLayerMax);
    }
}
