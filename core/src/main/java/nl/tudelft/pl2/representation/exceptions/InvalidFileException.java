package nl.tudelft.pl2.representation.exceptions;

/**
 * A class which represents an exception
 * that is related to files. Thus when something
 * goes wrong with the file, this exception must be thrown.
 */
public final class InvalidFileException extends CTagException {

    /**
     * The constructor for an InvalidFileException.
     *
     * This exception should be used when there is something wrong with the
     * file that was tried to be parsed. This includes when the file is locked
     * and thus cannot be read or when the file is in the wrong format.
     *
     * @param reason   A short explanation of what could be wrong with the file.
     * @param location A short explanation of where it could have gone wrong
     *                 in order to make it easier for debugging later.
     */
    public InvalidFileException(final String reason, final String location) {
        super(reason, location);
    }

    @Override
    public String toString() {
        return "An InvalidFileException was thrown! \n" + super.toString();
    }
}
