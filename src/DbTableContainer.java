import javax.swing.*;

/**
 * Speichert einen gesamten Tab nötig für {@link JTabbedPane tableTabbedPane} ({@link String tableName}, {@link JPanel},
 * {@link JScrollPane}, {@link JTable} für spätere Verwendung.
 */
public class DbTableContainer {

    public String tableName;
    public JScrollPane scrollPane;
    public JTable table;
}
