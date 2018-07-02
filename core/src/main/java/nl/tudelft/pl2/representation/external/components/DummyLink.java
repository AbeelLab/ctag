package nl.tudelft.pl2.representation.external.components;

import nl.tudelft.pl2.representation.external.Edge;

/**
 * Link that is used for drawing a dummy link in the graph.
 *
 * The dummy link references the link it is a dummy part of.
 * This means that when the information of the dummy link is
 * requested the info of the origin link is returned. This means
 * that all of the dummy links with the same origin link contain the
 * same data.
 */
public class DummyLink extends Edge {
    /**
     * The link that stores the info about the dummy link
     * that is in the optional part of the gfa file.
     *
     * If a link spans multiple layers it is broken down into
     * multiple dummy links. To reduce the amount of duplicate
     * data the dummy link contains a reference to the link it was
     * created for. When the UI requests data about the dummy link the
     * dummy link gets the data from its original (origin) link.
     */
    private Edge originLink;

    /**
     * Dummy used for drawing links in the graph.
     *
     * @param from The Segment the link comes from
     * @param to   The Segment the link goes to
     * @param edge The Link containing the info
     *             about the DummyLink
     */
    public DummyLink(
            final int from,
            final int to,
            final Edge edge) {
        super(from, to);
        assert !(edge instanceof DummyLink);
        this.originLink = edge;
    }

    /**
     * Get the origin link of the dummy link.
     *
     * @return The origin link
     */
    public final Edge origin() {
        assert !(originLink instanceof DummyLink);
        return originLink;
    }

    @Override
    public final boolean isDummy() {
        return true;
    }
}
