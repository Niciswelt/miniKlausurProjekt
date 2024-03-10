import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.*;

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
            // todo
            try {
                connect(connectTextField.getText());
                prepareTables();
                tabbedPane.setEnabledAt(1, true);
                tabbedPane.setEnabledAt(2, true);
            } catch (SQLException ex) {
                System.out.println("ERR  | " + ex);
                showMessage(ex.toString());
            }
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
    }

    private void connect(String dbName) throws SQLException {
        if (dbName.isEmpty()) return; // leerer Name -> return
        con = DriverManager.getConnection("jdbc:mysql://" + dbServer + ":" + dbPort + "/" + dbName, dbUser, dbPasswd);
    }

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

    private ResultSet passSQL(String sql) throws SQLException {
        Statement stmt = con.createStatement();
        return stmt.execute(sql) ? stmt.getResultSet() : null;
    }

    public Vector<String> nameTables() {
        Vector<String> nameTables = new Vector<>();
        String[] types = {"TABLE"};
        String catalogue = dbName;
        try{
            //Tabellen anfragen
            ResultSet rs = con.getMetaData().getTables(catalogue, null, null, types);
            while (rs.next()) { //dem Vektor die Namen hinzufügen
                nameTables.add(rs.getString(3));
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return nameTables;
    }

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