package nl.tudelft.pl2.representation.ui.InfoSidePanel;

/**
 * Created by Just on 23-5-2018.
 */
public class TableOption {

    /**
     * Name string of a node option.
     */
    private String name;

    /**
     * Value string of a node option.
     */
    private String value;

    /**
     * TableOption constructor.
     * @param initName name
     * @param initValue value
     */
    public TableOption(final String initName, final String initValue) {
        this.name = initName;
        this.value = initValue;
    }

    /**
     * Name getter.
     * @return name
     */
    public final String getName() {
        return name;
    }

    /**
     * Value getter.
     * @return value
     */
    public final String getValue() {
        return value;
    }
}
