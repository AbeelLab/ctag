package nl.tudelft.pl2.representation.external.components;

import nl.tudelft.pl2.representation.external.Bubble;
import nl.tudelft.pl2.representation.external.Indel;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.ui.UIHelper;
import scala.collection.immutable.HashMap;
import scala.collection.mutable.ListBuffer;

/**
 * Class that represents a dummy segment in the graph.
 */
public class DummyNode extends Node {
    /**
     * The width of the dummy node line.
     *
     * Since a dummy node is drawn as a line a width is needed for this line.
     */
    private double width;

    /**
     * Constructor for a dummy segment.
     *
     * @param id    The ID of the dummy
     * @param layer The layer of the dummy
     */
    public DummyNode(
            final int id,
            final int layer) {
        super(id, "", layer, "",
                new ListBuffer<>(),
                new ListBuffer<>(),
                new HashMap<>(),
                new HashMap<>());
        this.width = 1.0;
    }

    /**
     * Get the width of the dummy node line.
     *
     * @return The width of the line
     */
    public final double getWidth() {
        return this.width;
    }

    /**
     * Set the width of the dummy node line.
     *
     * @param w The new width
     */
    public final void setWidth(final double w) {
        this.width = w;
    }

    @Override
    public final boolean isDummy() {
        GraphHandle handle = UIHelper.getGraph();
        if (handle == null || this.incoming().length() != 1) {
            return true;
        }
        int incomingId = this.incoming().iterator().next().from();
        if (incomingId < 0) {
            return true;
        }
        Node node = handle.retrieveCache().retrieveNodeByID(incomingId);
        return !(node instanceof Bubble) && !(node instanceof Indel);
    }
}
