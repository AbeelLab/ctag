package nl.tudelft.pl2.representation.exceptions;

/**
 * An abstract representation of our
 * custom made exceptions.
 */
public abstract class CTagException extends RuntimeException {

    /**
     * A description of where it could possibly have
     * gone wrong.
     */
    private String location;

    /**
     * This is the abstract exception which our
     * custom exceptions should be extending.
     *
     * This class gives an easy way to create a
     * clear format when trying to pass the
     * error message to the user.
     *
     * @param rsn The reason why the exception was
     *            thrown.
     * @param loc The location at which the
     *            exception was thrown.
     */
    public CTagException(final String rsn, final String loc) {
        super(rsn);
        this.location = loc;
    }

    /**
     * This method should be used when constructing the
     * subclass toString methods. The name of that
     * exception should be pre-pended to this message
     * in order to give the user more information.
     *
     * @return A string containing a formatted string with
     * the information regarding the reason it was thrown
     * and where it was thrown.
     */
    @SuppressWarnings("checkstyle:DesignForExtension")
    @Override
    public String toString() {
        String str = "";

        str += "The exception was thrown by: ";
        str += location;
        str += "\n";

        str += "It was caused by: ";
        str += super.getMessage();
        str += "\n";

        return str;
    }

}


