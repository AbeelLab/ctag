package nl.tudelft.pl2.representation.exceptions;

/**
 * The exception thrown when we cannot find a Segment.
 */
public class NodeNotFoundException extends CTagException {
    /**
     * The constructor for the exception when a segment
     * could not be found.
     *
     * @param rsn The reason why the exception was
     *            thrown.
     * @param loc The location at which the
     */
    public NodeNotFoundException(final String rsn, final String loc) {
        super(rsn, loc);
    }
}
