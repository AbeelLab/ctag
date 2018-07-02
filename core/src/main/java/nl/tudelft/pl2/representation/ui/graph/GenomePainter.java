package nl.tudelft.pl2.representation.ui.graph;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.ui.InfoSidePanel.SampleSelectionController;
import nl.tudelft.pl2.representation.ui.UIHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that colors the nodes if they
 * contain one of the selected genomes.
 */
public class GenomePainter {
    /**
     * The graphHandle the painter belongs to.
     */
    private GraphHandle graphHandle;

    /**
     * Map that maps node id to the color of said node.
     */
    private HashMap<Integer, Set<Color>> colorMap;

    /**
     * Genome color map.
     */
    private HashMap<String, Color> genomeColorMap;

    /**
     * Lock for the colorMap.
     */
    private Lock colorMapLock;

    /**
     * NodeDrawer for the graph that is loaded.
     */
    private NodeDrawer nodeDrawer;


    /**
     * The genome painter that colors the nodes.
     *
     * @param graph  The graph this painter belongs to
     * @param drawer The nodeDrawer that draws the nodes
     */
    public GenomePainter(final GraphHandle graph,
                         final NodeDrawer drawer) {
        this.colorMap = new HashMap<>();
        this.colorMapLock = new ReentrantLock();
        this.genomeColorMap = new HashMap<>();
        this.graphHandle = graph;
        this.nodeDrawer = drawer;
    }

    /**
     * Add the string to the selected genomes set.
     *
     * @param genome The new selected sample
     * @param color  Color of the sample.
     */
    public final void addSelected(final String genome, final Color color) {
        try {
            colorMapLock.lock();

            genomeColorMap.put(genome, color);

            graphHandle.getNodesByGenome(genome).forEach(node -> {
                if (colorMap.containsKey(node.id())) {
                    colorMap.get(node.id()).add(color);
                } else {
                    Set<Color> colorSet = new HashSet<>();
                    colorSet.add(color);
                    colorMap.put(node.id(), colorSet);
                }
            });
        } finally {
            colorMapLock.unlock();
        }
        if (graphHandle.isLoaded()) {
            try {
                Platform.runLater(() -> nodeDrawer.redrawGraph());
            } catch (IllegalStateException e) {
                nodeDrawer.redrawGraph();
            }
        }
    }


    /**
     * Remove a sample from the selected genomes set.
     *
     * @param genome The sample to remove
     * @param color  color of the genome.
     */
    public final void removeSelected(final String genome, final Color color) {

        genomeColorMap.remove(genome);

        try {
            colorMapLock.lock();
            graphHandle.getNodesByGenome(genome).forEach(node -> {
                if (colorMap.containsKey(node.id())) {
                    Set<Color> colors = colorMap.get(node.id());
                    if (colors.contains(color)) {
                        colors.remove(color);
                    }
                }
            });
        } finally {
            colorMapLock.unlock();
        }
        try {
            Platform.runLater(() -> nodeDrawer.redrawGraph());
        } catch (IllegalStateException e) {
            nodeDrawer.redrawGraph();
        }
    }

    /**
     * Get the color of a node by its id.
     *
     * @param id The id of the node
     * @return The color of the node
     */
    public final Set<Color> getColorById(final int id) {
        try {
            colorMapLock.lock();
            Set<Color> set = new HashSet<>();
            set.add(Color.web("#f4f4f4"));
            return colorMap.getOrDefault(id, set);
        } finally {
            colorMapLock.unlock();
        }
    }

    /**
     * Adds a new node to the color map.
     *
     * @param node Node
     * @return If a color was assigned
     */
    public final boolean addNewNode(final Node node) {
        boolean found = false;
        if (genomeColorMap.size() != 0) {
            Map<String, String> options = node.getOptions();

            if (options.containsKey(
                    SampleSelectionController.GENOME_TAG)) {
                String[] sampleStrings =
                        convertIndexToGenome(options
                                .get(SampleSelectionController.GENOME_TAG)
                                .split(";"));

                for (String sample : sampleStrings) {
                    if (genomeColorMap.containsKey(sample)) {
                        if (colorMap.containsKey(node.id())) {
                            found = true;
                            colorMap.get(node.id())
                                    .add(genomeColorMap.get(sample));
                        } else {
                            found = true;
                            Set<Color> colorSet = new HashSet<>();
                            colorSet.add(genomeColorMap.get(sample));
                            colorMap.put(node.id(), colorSet);
                        }
                    }
                }
            }
        }
        return found;
    }

    /**
     * Converts index strings to genome names.
     *
     * @param strings Index strings.
     * @return Genome name strings
     */
    private String[] convertIndexToGenome(final String[] strings) {
        String[] converted = new String[strings.length];
        String[] genomes = UIHelper.getGraph().getGenomes();
        for (int i = 0; i < strings.length; i++) {
            try {
                int index = Integer.parseInt(strings[i]);
                converted[i] = genomes[index];
            } catch (NumberFormatException e) {
                return strings;
            }
        }
        return converted;
    }

    /**
     * Removes node from color map.
     *
     * @param node Node
     * @return If a node was removed
     */
    public final boolean removeNode(final Node node) {
        return colorMap.remove(node.id()) != null;
    }

    /**
     * Get the graphHandle this painter belongs to.
     *
     * @return The graphHandle
     */
    public final GraphHandle getGraphHandle() {
        return graphHandle;
    }

}
