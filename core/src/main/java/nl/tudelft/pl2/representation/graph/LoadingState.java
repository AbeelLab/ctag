package nl.tudelft.pl2.representation.graph;

/**
 * Represents the loading state of a class.
 */
public enum LoadingState {
    /**
     * The graph is fully loaded.
     */
    FULLY_LOADED,
    /**
     * The gff file is fully loaded.
     */
    FULLY_LOADED_GFF,
    /**
     * A chunk has been loaded.
     */
    CHUNK_LOADED,
    /**
     * A milestone for parsing.
     */
    MILESTONE,
    /**
     * The parsing process is done.
     */
    FULLY_PARSED,
    /**
     * Notification that the index has been written.
     */
    INDEX_WRITTEN,

    /**
     * Read header file.
     */
    HEADERS_READ,

    /**
     * Read heatmap file.
     */
    HEATMAP_READ,

    /**
     * Loaded caches.
     */
    CACHES_LOADED,

    /**
     * Files are present.
     */
    FILES_PRESENT
}
