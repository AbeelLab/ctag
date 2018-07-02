package nl.tudelft.pl2.representation.ui.bookmark;

import org.junit.Test;

import java.util.NoSuchElementException;

/**
 * Class for testing the bookmark string converter.
 */
public class BookmarkStringConverterTest {
    /**
     * If no graph is loaded no bookmark can be created from a string.
     */
    @Test(expected = NoSuchElementException.class)
    public void testBookmarkConverter() {
        final String bookmarkString = "BK:TB10v2.gfa;Z:5;T:-1.6666666666666667;N:[0, 1, 2, 3];DESC:Test;";
        BookmarkStringConverter converter = new BookmarkStringConverter();
        converter.fromString(bookmarkString);
    }
}
