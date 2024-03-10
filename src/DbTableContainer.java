import javax.swing.*;
import java.util.Calendar;

/**
 * Speichert einen gesamten Tab nötig für {@link JTabbedPane tableTabbedPane} ({@link String tableName}, {@link JPanel},
 * {@link JScrollPane}, {@link JTable}, {@link Calendar lastUpdated}) für spätere Verwendung.
 */
public class DbTableContainer {

    public String tableName;
    public JScrollPane scrollPane;
    public JTable table;
    public Calendar lastUpdated;
}
