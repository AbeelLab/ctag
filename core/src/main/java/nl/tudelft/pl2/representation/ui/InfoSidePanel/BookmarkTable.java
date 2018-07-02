package nl.tudelft.pl2.representation.ui.InfoSidePanel;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import nl.tudelft.pl2.representation.ui.bookmark.Bookmark;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Created by Just on 18-6-2018.
 */
public class BookmarkTable {

    /**
     * Bookmark object.
     */
    private Bookmark bookmark;

    /**
     * Font awesome font construction.
     */
    private GlyphFont awesome = GlyphFontRegistry.font("FontAwesome");

    /**
     * Delete glyph used as button to remove bookmarks from table.
     */
    private SimpleObjectProperty<Glyph> deleteGlyph =
            new SimpleObjectProperty<>(awesome
                    .create(FontAwesome.Glyph.TIMES_CIRCLE).color(Color.RED));

    /**
     * Upload glyph used to copy bookmark to clipboard.
     */
    private SimpleObjectProperty<Glyph> uploadGlyph =
            new SimpleObjectProperty<>(awesome
                    .create(FontAwesome.Glyph.UPLOAD).color(Color.BLACK));


    /**
     * Constructor of bookmarkTable.
     * @param bm Bookmark
     */
    BookmarkTable(final Bookmark bm) {
        bookmark = bm;
    }

    /**
     * Bookmark getter.
     * @return Bookmark
     */
    public final Bookmark getBookmark() {
        return bookmark;
    }


    /**
     * DeleteGlyphProperty getter.
     * @return SimpleObjectProperty
     */
    public final SimpleObjectProperty<Glyph> deleteGlyphProperty() {
        return deleteGlyph;
    }


    /**
     * UploadGLyphProperty getter.
     * @return SimpleObjectProperty
     */
    public final SimpleObjectProperty<Glyph> uploadGlyphProperty() {
        return uploadGlyph;
    }

}
