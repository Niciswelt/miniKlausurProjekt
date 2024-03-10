import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class DbHelper extends JFrame {

    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private JPanel connectorPanel;
    private JTextField connectTextField;
    private JButton connectButton;
    private JPanel dbManagerPanel;
    private JTabbedPane tableTabbedPane;
    private JButton deleteTupleButton;
    private JButton refreshButton;
    private JScrollPane addScrollPane;
    private JPanel addPanel;
    private JButton addTupleButton;
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
    List<JTextField> addTupleTextFields;

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
        addTupleTextFields = new ArrayList<>();
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
                connect(connectTextField.getText());
                prepareTables();

                tabbedPane.setEnabledAt(1, true);
                tabbedPane.setEnabledAt(2, true);

                this.setTitle("Database Manager - " + dbName); // aktualisiert Fenstername
            } catch (SQLException ex) {
                System.out.println("ERR  | " + ex);
                showMessage(ex.toString());
            }
        });

        // table tabbed pane
        tableTabbedPane.setBorder(new EmptyBorder(5,5,5,5));
        tableTabbedPane.addChangeListener(e -> {
            int index = tableTabbedPane.getSelectedIndex(); // index aktueller Tabelle
            if (index == -1) return;
            if (index > databaseTablePanels.size()-1) return; // wenn selektierte Tab über Anzahl der Tabellen, return
            System.out.println("INFO | Selected index " + index);
            selectTable(index);
        });

        // addPanel
        addScrollPane.setViewportView(addPanel);

        // addTupleButton
        addTupleButton.setFocusable(false);
        addTupleButton.setEnabled(false);
        addTupleButton.addActionListener(e -> {
            int index = tableTabbedPane.getSelectedIndex();
            String tableName = tableTabbedPane.getTitleAt(index);

            // Baut SQL-Befehl
            String sql = "INSERT INTO `" + tableName + "` VALUES (";
            StringBuilder stringBuilder = new StringBuilder(sql); // 'INSERT INTO `tableName` VALUES ("'
            for (JTextField addTupleTextField : addTupleTextFields) { // 'attributeValue1, attributeValue2,' ...
                String attributeValue = addTupleTextField.getText();
                stringBuilder.append("'").append(attributeValue).append("',");
                addTupleTextField.setText(""); // jeweiliges Textfeld leeren für weitere Nutzung
            }
            stringBuilder.deleteCharAt(stringBuilder.length()-1).append(");"); // letztes Komma entfernen, ');' hinzufügen

            // Führt SQL-Befehl aus
            try {
                passSQL(new String(stringBuilder));
                refreshTable(index); // Anzeige wird nach Ausführung aktualisiert
            } catch (SQLException ex) {
                System.out.println("ERR  | " + ex);
                JOptionPane.showMessageDialog(null, "ERROR: "+ex);
            }
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
        return stmt.execute(sql) ? stmt.getResultSet() : null;
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
     * @param index der zu anzuzeigenden Tabelle
     */
    void selectTable(int index) {
        DbTableContainer dtc = databaseTablePanels.get(index);

        if(!dtc.table.isEnabled()) { //erste Ansicht der Tabelle
            refreshTable(index); //erstellt die Tabelle
            dtc.table.setEnabled(true); //aktiviert table, um die Erstellung nachvollziehen zu können
            prepareAdd(dtc.table); //richtet addPanel ein
            return;
        }
        else {
            System.out.println("INFO | loaded last known data for " + index);
        }

        prepareAdd(dtc.table); //richtet addPanel ein
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
    void prepareAdd(JTable table) {
        // Aktuelle Anzeige löschen
        addPanel.removeAll();
        addTupleTextFields.clear();

        // Spaltennamen erhalten
        int columnCount = table.getColumnCount();
        String[] values = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            values[i] = table.getColumnName(i);
        }

        // Layout
        addPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Komponenten hinzufügen
        for (int i = 0; i < values.length; i++) {
            // Labels hinzufügen
            gbc.gridx = 0;
            gbc.gridy = i;
            addPanel.add(new JLabel(values[i]), gbc);

            // Textfelder zu Panel und Liste hinzufügen
            gbc.gridx = 1;
            JTextField textField = new JTextField(30);
            addPanel.add(textField, gbc);
            addTupleTextFields.add(textField); // speichert die Textfelder, um später den Text zu entnehmen
        }
        // Spalten nach oben ausrichten
        gbc.gridx = 0;
        gbc.gridy = values.length;
        gbc.weighty = 1.0;
        addPanel.add(Box.createVerticalGlue(), gbc);

        addTupleButton.setEnabled(true);
        addPanel.repaint();
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

        Vector<Integer> foreignColumns = new Vector<>(); // Vektor, welches die Indexe der Spalten mit Fremdschlüssel enthält
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            if (rsmd.getColumnName(i).startsWith(foreignPrefix)) foreignColumns.add(i);
        }

        if (foreignColumns.isEmpty()) return rs; // kein INNER JOIN notwendig, returned das ursprüngliche ResultSet

        // 'SELECT tableName.id,'
        StringBuilder builder = new StringBuilder("SELECT " + tableName + "." + primaryKey + ",");

        // 'attribute1, attribute2,' ... (die Attribute sind Fremdschlüssel)
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