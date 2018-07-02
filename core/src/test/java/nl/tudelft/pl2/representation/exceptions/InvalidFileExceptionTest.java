package nl.tudelft.pl2.representation.exceptions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class InvalidFileExceptionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testThrowingException() throws InvalidFileException {
        thrown.expect(InvalidFileException.class);

        throw new InvalidFileException("bla", "bla");
    }


    @Test
    public void testToString() {
        InvalidFileException exception = new InvalidFileException("Bla", "blala");

        assertThat(exception.toString()).isEqualTo("An InvalidFileException was thrown! \n" +
                "The exception was thrown by: blala\n" +
                "It was caused by: Bla\n");
    }
}
