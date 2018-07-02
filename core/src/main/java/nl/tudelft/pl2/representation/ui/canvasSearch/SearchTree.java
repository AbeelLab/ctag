package nl.tudelft.pl2.representation.ui.canvasSearch;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Rectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rx.Observable;

import java.util.function.Consumer;

import static com.github.davidmoten.rtree.geometry.Geometries.point;
import static com.github.davidmoten.rtree.geometry.Geometries.rectangle;


/**
 * A r-tree wrapper class.
 * The search tree is used to efficiently find a node based on
 * its coordinates.
 * Adding a node to the r-tree takes in the worst case O(n)
 * Finding a node takes O(log(n))
 *
 * @param <T> The type of elements stored in the searchtree.
 */
public class SearchTree<T> {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER
            = LogManager.getLogger("SearchTree");

    /**
     * r-tree from davidmoten.rtree package.
     */
    private RTree<T, Rectangle> tree;

    /**
     * NodeSearchTree constructor creates a r-tree.
     */
    public SearchTree() {
        tree = RTree.create();
    }


    /**
     * Adds the canvas coordinates of a node to
     * an immutable r-tree with the node id as reference.
     *
     * @param id The ID of a node
     * @param x  x-coordinate of the node
     * @param y  y-coordinate of the node
     * @param x2 highest x-coordinate of the node
     * @param y2 highest y-coordinate of the node
     */
    public final void add(final T id, final int x, final int y,
                          final int x2, final int y2) {
        tree = tree.add(id,
                rectangle(x, y, x2, y2));
    }


    /**
     * Searches for a node with as input a point.
     * If the point intersects with a node,
     * a consumer function is executed on the node's id.
     * If no element is found, a NoSuchElementException is caught
     * and a message is printed.
     *
     * @param x                x coordinate of search point.
     * @param y                y coordinate of search point.
     * @param functionFound    Consumer function that takes as input the id
     *                         of the node. The consumer is run whenever a
     *                         node is found in the search tree.
     * @param functionNotFound The consumer function when nothing is found.
     */
    public final void search(final int x, final int y,
                             final Consumer<Object> functionFound,
                             final Consumer<Object> functionNotFound) {
        Observable<Entry<T, Rectangle>> node = tree.search(point(x, y))
                .firstOrDefault(null);

        node.forEach((entry) -> {
            if (entry != null) {
                functionFound.accept(entry.value());
            } else {
                functionNotFound.accept("");
                LOGGER.info("Could not find RTree entry at x={}, y={}.", x, y);
            }
        });
    }

    /**
     * Get the RTree used in the search tree.
     *
     * @return The RTree of the search tree
     */
    public final RTree<T, Rectangle> getTree() {
        return tree;
    }

    /**
     * Clears the search tree while remaining the reference to
     * this SearchTree.
     */
    public final void clear() {
        tree = null;
        tree = RTree.create();
    }

}
