package nl.tudelft.pl2.representation.ui;

import nl.tudelft.pl2.data.GraphUpdateCompleter;
import nl.tudelft.pl2.representation.graph.loaders.ChunkedGraphLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

/**
 * Class that provides an entry point for the command line tool.
 */
public final class CommandLineTool {
    /**
     * Boolean to check if the loading has been done.
     */
    private static boolean loaded = false;

    /**
     * Log4J [[Logger]] used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("CommandLineTool");

    /**
     * Character length of the .gfa extension.
     */
    private static final int EXT_LENGTH = 4;

    /**
     * CommandLine tool should not be instantiated.
     */
    private CommandLineTool() {
    }

    /**
     * Main method.
     *
     * @param args The arguments of the program
     */
    public static void main(final String[] args) {
        if (args.length != 1) {
            LOGGER.error("The program should be called "
                    + "with only a file path as an argument");
            return;
        }
        String pathString = args[0];
        File file = new File(pathString);
        String fileName = file.getName();
        String extension = fileName.substring(
                fileName.length() - EXT_LENGTH, fileName.length()
        );
        if (!extension.equals(".gfa")) {
            LOGGER.error("Invalid file type, gfa file expected");
            return;
        }
        Path path = Paths.get(file.toURI());
        ChunkedGraphLoader chunkedGraphLoader =
                new ChunkedGraphLoader();
        chunkedGraphLoader.load(path, new GraphUpdateCompleter(file));
        chunkedGraphLoader.getLoadFutures().forEach((future) -> {
            try {
                future.get();
            } catch (InterruptedException
                    | ExecutionException
                    | IllegalStateException e) {
                e.printStackTrace();
            }
        });
        loaded = true;
        LOGGER.info("Done creating index");
    }

    /**
     * Return whether the command line is done loading.
     *
     * @return If the command line tool has loaded the files.
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
