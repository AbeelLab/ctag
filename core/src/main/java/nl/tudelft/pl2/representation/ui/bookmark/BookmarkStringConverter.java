package nl.tudelft.pl2.representation.ui.bookmark;

import javafx.util.StringConverter;

import java.util.List;

/**
 * The class which converts a bookmark to a string.
 */
public class BookmarkStringConverter extends StringConverter<Bookmark> {
    /**
     * Converts the bookmark into its description
     * to make it human readable.
     * @param object The bookmark which is to be converted
     *               to a string.
     *
     * @return a string representation of the bookmark passed in.
     */
    @Override
    public final String toString(final Bookmark object) {
        return object.description();
    }

    /**
     * Converts the string provided into a bookmark defined by
     * that description.
     * @param string The description of the bookmark.
     *
     * @return a bookmark representation of the string passed in.
     */
    @Override
    public final Bookmark fromString(final String string) {
        List<Bookmark> bookmarks = BookmarkManager.getBookmarks();
        return bookmarks.stream().filter(bookmark ->
                bookmark.description().equals(string)).findFirst().get();
    }

}
