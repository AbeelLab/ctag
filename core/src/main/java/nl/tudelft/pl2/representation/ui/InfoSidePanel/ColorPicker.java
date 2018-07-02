package nl.tudelft.pl2.representation.ui.InfoSidePanel;

import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Class used to get unique Colors.
 */
public class ColorPicker {

    /**
     * Set of colors.
     */
    private Set<Color> colors;

    /**
     * Max number for rgb numbers.
     */
    private static final int[] COLORINTS =
            {25, 50, 75, 100, 125, 150, 175, 200, 225, 255};

    /**
     * Random number generator.
     */
    private Random rnd = new Random();

    /**
     * Constructor.
     */
    public ColorPicker() {
        colors = new HashSet<>();
    }

    /**
     * Returns an unique color.
     * @return Color
     */
    public final Color newColor() {
        Color c = rndColor();
        while (colors.contains(c)) {
            c = rndColor();
        }
        return c;
    }

    /**
     * Removes color from set.
     * @param c Color
     */
    public final void removeColor(final Color c) {
        colors.remove(c);
    }


    /**
     * Returns a random color.
     * @return Color
     */
    private Color rndColor() {
        return Color.rgb(
                COLORINTS[rnd.nextInt(COLORINTS.length)],
                COLORINTS[rnd.nextInt(COLORINTS.length)],
                COLORINTS[rnd.nextInt(COLORINTS.length)]
        );
    }


}
