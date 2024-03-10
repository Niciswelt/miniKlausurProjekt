import javax.swing.table.DefaultTableModel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

public class TableHelper {

    /**
     * Erstellt aus einem {@link ResultSet} Objekt ein entsprechendes {@link DefaultTableModel}
     * @param result {@link ResultSet} zur Erzeugung des TableModels
     * @return entsprechendes {@link DefaultTableModel}
     * @throws SQLException wenn ein Fehler bei der Datenbankeninteraktion auftritt oder die Verbindung geschlossen ist
     */
    public static DefaultTableModel getTableModel(ResultSet result) throws SQLException {
        // Erzeuge neues DefaultTableModel und gib es zurück
        return new DefaultTableModel(getData(result), getColumnNames(result));
    }

    private static Vector<String> getColumnNames(ResultSet result) throws SQLException {
        // Erzeuge leeren Vektor
        Vector<String> columnNames = new Vector<>();
        // Suche Anzahl an Attributen
        int attributes = result.getMetaData().getColumnCount();

        // Füge Attributnamen dem Vektor hinzu
        // Merke: Spaltenindizierung startet bei 1!
        for (int cIndex = 1; cIndex <= attributes; cIndex++) {
            columnNames.add(result.getMetaData().getColumnName(cIndex));
        }

        return columnNames;
    }

    private static Vector<Vector<Object>> getData(ResultSet result) throws SQLException {
        // Beinhaltet alle Zeilen-Vektoren
        // Vector<Object> ==> Wir wissen nicht, welcher Datentyp dahinter steckt
        Vector<Vector<Object>> data = new Vector<>();
        // Speichert die Anzahl an Spalten ==> Anzahl an Attributen der Tabelle
        int attributes = result.getMetaData().getColumnCount();

        // Solange noch weitere Zeilen existieren...
        while(result.next()) {
            // Erzeuge einen leeren Vektor
            Vector<Object> rowVector = new Vector<>();

            // Füge alle Daten dieser Reihe dem Vektor hinzu
            // Merke: Spaltenindizierung startet bei 1!
            for (int i = 1; i <= attributes; i++) {
                // Verwendung von getObject() weil: z.B. getInt() auf einem String resultiert in Exception
                rowVector.add(result.getObject(i));
            }
            // Füge Reihenvektor hinzu
            data.add(rowVector);
        }

        return data;
    }
}
