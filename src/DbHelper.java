import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class DbHelper extends JFrame {

    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private JPanel connectorPanel;
    private JTextField connectTextField;
    private JButton connectButton;
    private JTabbedPane tableTabbedPane;
    private JButton deleteTupleButton;
    private JButton refreshButton;
    private JScrollPane sqlAreaScrollPane;
    private JTextArea sqlTextArea;
    private JButton executeButton;
    private JButton clearButton;
    private JPanel sqlResultPanel;
    private JScrollPane sqlResultScrollPane;
    private JPanel sqlPanel;


    // Datenbank Einstellungen
    String dbServer = "localhost";
    String dbPort = "3306"; //Datenbankport [Standard: 3306]
    String dbUser = "root"; //Datenbankserver Benutzername
    String dbPasswd = "";  //Datenbankserver Passwort
    String dbName = "spiele_bg20"; //Datenbankname

    Connection con;

    // Andere
    List<DbTableContainer> databaseTablePanels;

    DbHelper() {
        initialize();
        createComponents();
        connectTextField.grabFocus();
        connectTextField.selectAll();
    }

    private void initialize() {
        this.setContentPane(mainPanel);
        this.setTitle("Database Manager");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(0,0,1600,800);
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        databaseTablePanels = new ArrayList<>();
    }

    private void createComponents() {
        mainPanel.setBorder(new EmptyBorder(5,5,5,5));

        // tabbedPane
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setEnabledAt(2, false);

        // connectTextField
        connectTextField.setText(dbName); // Standard Datenbank einsetzen
        connectTextField.addActionListener(e -> connectButton.doClick()); // Für 'ENTER' Eingabe

        // connectButton
        connectButton.setFocusable(false);
        connectButton.addActionListener(e -> {
            try {
                dbName = connectTextField.getText();
                connect(dbName);
                prepareTables();

                deleteTupleButton.setEnabled(false);

                tabbedPane.setEnabledAt(1, true);
                tabbedPane.setEnabledAt(2, true);

                this.setTitle("Database Manager - " + dbName); // aktualisiert Fenstername
            } catch (SQLException ex) {
                System.out.println("ERR  | " + ex);
                showMessage(ex.toString());
            }
        });

        //delete Button: Löscht ausgewählte Zeile der Datenbanktabelle
        deleteTupleButton.setFocusable(false);
        deleteTupleButton.addActionListener(e -> {
            int index = tableTabbedPane.getSelectedIndex();
            String tableName = tableTabbedPane.getTitleAt(index);

            JTable table = databaseTablePanels.get(index).table; // aktuelle Tabelle
            int row = table.getSelectedRow(); // ausgewählte Zeile
            if (row == -1) return; // return, wenn keine Zelle ausgewählt
            String columnName = table.getColumnName(0); // Name der ersten Spalte (meist ID)
            String id = table.getValueAt(row, 0).toString(); // ruft ID der ausgewählten Zeile ab

            // Baut SQL-Befehl
            String sql = "DELETE FROM " + tableName + " WHERE " + tableName + "." + columnName + " = " + id;

            // Führt SQL-Befehl aus
            try {
                passSQL(sql);
                refreshTable(index); // aktualisiert Anzeige nach Löschung
            } catch (SQLException ex) {
                System.out.println(ex);;
                JOptionPane.showMessageDialog(null, "ERROR: "+ex);
            }
        });
        //refresh Button: Aktualisiert Tabellenanzeige
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(e -> {
            int index = tableTabbedPane.getSelectedIndex(); // aktuelle Tabelle
            refreshTable(index);
        });

        // table tabbed pane
        tableTabbedPane.setBorder(new EmptyBorder(5,5,5,5));
        tableTabbedPane.addChangeListener(e -> {
            int index = tableTabbedPane.getSelectedIndex(); // index aktueller Tabelle
            if (index == -1) return;
            if (index > databaseTablePanels.size()-1) return; // wenn selektierte Tab über Anzahl der Tabellen, return
            System.out.println("INFO | Selected index " + index);
            selectTable(index);

            refreshButton.setEnabled(true);
            deleteTupleButton.setEnabled(true);
        });

        // SQL tab
        // sqlResultArea
        sqlResultPanel.setVisible(false);

        // executeButton
        executeButton.setFocusable(false);
        executeButton.addActionListener(e -> {
            try {
                ResultSet rs = passSQL(sqlTextArea.getText());

                if (rs == null) return;
                sqlResultScrollPane.setViewportView(new JTable(TableHelper.getTableModel(rs)));
                sqlResultPanel.setVisible(true);
            } catch (SQLException ex) {
                System.out.println("ERR  | " + ex);
                showMessage(ex.toString());
            }
        });

        // clearButton
        clearButton.setFocusable(false);
        clearButton.addActionListener(e -> sqlTextArea.setText(""));
    }

    /**
     * Stellt eine Verbindung zur übergebenen Datenbank her.
     * @param dbName {@link String} zur identifizierung der Datenbank
     * @throws SQLException, wenn ein Fehler bei der Verbindung zur Datenbank auftritt
     */
    private void connect(String dbName) throws SQLException {
        if (dbName.isEmpty()) return; // leerer Name -> return
        con = DriverManager.getConnection("jdbc:mysql://" + dbServer + ":" + dbPort + "/" + dbName, dbUser, dbPasswd);
        System.out.println("Connected to " + dbName);
    }

    /**
     * Erstellt Tabs für die Datenbanktabellen der verbundenen Datenbank und erstellt jeweils einen {@link DbTableContainer}.
     */
    private void prepareTables() {
        tableTabbedPane.removeAll();
        databaseTablePanels.clear();

        Vector<String> tableNames = nameTables();

        for (int i = 0; i < tableNames.size(); i++) {
            String tableName = tableNames.get(i);

            DbTableContainer dtc = new DbTableContainer();
            JScrollPane scrollPane = new JScrollPane();
            JTable table = new JTable();

            table.setEnabled(false); // vorerst disabled, um später erkennen zu können, ob die Tabelle gefüllt ist
            table.setName(tableName);
            scrollPane.setViewportView(table);

            // Zum DbTableContainer hinzufügen
            dtc.tableName = tableName;
            dtc.table = table;
            dtc.scrollPane = scrollPane;

            tableTabbedPane.add(dtc.scrollPane); // erstellt Tab mithilfe eines JScrollPanes
            tableTabbedPane.setTitleAt(i, dtc.tableName);
            databaseTablePanels.add(dtc); // fügt DbTableContainer zur Liste hinzu
        }
    }

    /**
     * Führt eine SQL-Abfrage durch.
     * @param sql {@link String} mit SQL-Befehl, der ausgeführt werden soll
     * @return Ein entsprechendes {@link ResultSet} oder null, falls keines vorhanden
     * @throws SQLException, wenn ein Fehler bei der Datenbankinteraktion auftritt oder die Verbindung geschlossen ist
     */
    private ResultSet passSQL(String sql) throws SQLException {
        Statement stmt = con.createStatement();
        System.out.println("INFO | Executing SQL:\n" + sql);
        return stmt.execute(sql) ? stmt.getResultSet() : null; // = if(stmt.execute(sql)), dann return getResultSet, sonst return null
    }

    /**
     * Gibt alle Datenbanktabellennamen der aktuellen Verbindung zurück.
     * @return einen {@link Vector<String>} mit den Namen der Datenbanktabellen.
     */
    public Vector<String> nameTables() {
        Vector<String> nameTables = new Vector<>();
        String[] types = {"TABLE"};
        String catalogue = dbName;
        try{
            //Tabellen anfragen
            ResultSet rs = con.getMetaData().getTables(catalogue, null, null, types);
            while (rs.next()) { //dem Vektor die Namen hinzufügen
                nameTables.add(rs.getString(3)); //columnIndex 3 eines ResultSets ist der Tabellenname
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return nameTables;
    }

    /**
     * Wird beim Auswählen einer Tabelle ausgeführt und bestimmt, ob {@link #refreshTable(int index)} verwendet werden
     * muss.
     * <p>
     * Dies trifft zu bei der ersten Anfrage, eine Datenbanktabelle anzuzeigen
     * @param index der anzuzeigenden Tabelle
     */
    void selectTable(int index) {
        DbTableContainer dtc = databaseTablePanels.get(index);

        if(!dtc.table.isEnabled()) { //erste Ansicht der Tabelle
            refreshTable(index); //erstellt die Tabelle
            dtc.table.setEnabled(true); //aktiviert table, um die Erstellung nachvollziehen zu können
        }
        else System.out.println("INFO | loaded last known data for " + index);
    }

    /**
     * Aktualisiert die aktuelle Tabelle.
     * @param index der zu aktualisierenden Tabelle
     */
    private void refreshTable(int index) {
        DbTableContainer dtc = databaseTablePanels.get(index);
        try {
            ResultSet rs = passSQL("SELECT * FROM " + dtc.tableName);
            rs = discloseForeignKeys(rs); // Fremdschlüssel bei Bedarf entschlüsseln

            // tableModel holen und anwenden
            DefaultTableModel model = TableHelper.getTableModel(rs);
            dtc.table.setModel(model);

            tableTabbedPane.repaint(); // stellt Richtigkeit des UIs sicher
        } catch (SQLException ex){
            System.out.println("ERR | " + ex);
        }
    }

    /**
     * Führt ein INNER JOIN aus, um den eigentlichen Wert von Fremdschlüssel darzustellen.
     * @param rs {@link ResultSet}, in welches die Fremdschlüssel entschlüsselt werden sollen
     * @return {@link ResultSet} des INNER JOIN, wenn eines ausgeführt wurde, sonst das Übergebene
     * @throws SQLException, wenn ein Fehler bei der Datenbankinteraktion auftritt oder die Verbindung geschlossen ist
     */
    private ResultSet discloseForeignKeys(ResultSet rs) throws SQLException {
        String primaryKey = "id"; // Bezeichnung für Hauptschlüssel
        String foreignPrefix = "F_"; // Prefix für Fremdschlüssel

        ResultSetMetaData rsmd = rs.getMetaData();
        String tableName = rsmd.getTableName(1); // Datenbanktabellenname

        // 'SELECT tableName.id,'
        StringBuilder builder = new StringBuilder("SELECT " + tableName + ".");

        // 'attribute1, attribute2,' ...
        Vector<Integer> foreignColumns = new Vector<>(); // Vektor, welches die Indexe der Spalten mit Fremdschlüssel enthält
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            String columnName = rsmd.getColumnName(i);
            if (columnName.startsWith(foreignPrefix)) foreignColumns.add(i);
            else builder.append(columnName).append(",");
        }

        if (foreignColumns.isEmpty()) return rs; // kein INNER JOIN notwendig, returned das ursprüngliche ResultSet

        // 'f_attribute1,f_attribute2,' ...
        for (int columnIndex : foreignColumns) {
            String foreignColumn = rsmd.getColumnName(columnIndex).replace(foreignPrefix, ""); // ohne 'F_'
            builder.append(foreignColumn).append(",");
        }

        // löscht das letzte Komma, 'FROM tableName'
        builder.deleteCharAt(builder.length()-1).append("\nFROM ").append(tableName);

        // 'INNER JOIN attribute1 ON tableName.F_attribute1 = attribute1.id'
        for (int columnIndex : foreignColumns) {
            String foreignColumnLiteral = rsmd.getColumnName(columnIndex); // vollständiger Spaltenname, z.B. 'F_Bewertung'
            String foreignColumn = foreignColumnLiteral.replace(foreignPrefix, ""); // ohne 'F_'
            builder.append("\nINNER JOIN ").append(foreignColumn).append("\n");
            builder.append("ON ").append(tableName).append(".").append(foreignColumnLiteral).append(" = ").append(foreignColumn).append(".").append(primaryKey);
        }

        return passSQL(new String(builder)); // führt INNER JOIN aus und returned das ResultSet
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(null, msg);
    }
}
