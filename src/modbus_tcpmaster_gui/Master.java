package modbus_tcpmaster_gui;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.TCPMasterConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
pokrecemo nekoliko TCPSlave uredjaja sa "MODBUS_TCPSlave" te, preko konfiguracijske JSON datoteke, citamo njihove podateke(ip, port, unitID)
te ih prikazujemo na ekranu.
Defaultna adresna vrijednost je 0, no imamo mogucnost postaviti pocetak adresnog range-a nakon cega se prikazuju vrijednosti
Program dinamicki otvara prozor na pocetku, ovisno o tome koliko imamo SLAVE uredjaja
*/

public class Master extends JFrame {

    private TCPMasterConnection con = null; //the connection
    private ModbusTCPTransaction trans = null; //the transaction
    private ReadMultipleRegistersRequest req = null; //the request
    private ReadMultipleRegistersResponse res = null; //the response
    private final Timer timer;
    private List<Connection> C = new ArrayList<>();
    private List<JTable> tables = new ArrayList<>();
    private List<JButton> buttons = new ArrayList<>();
    private List<JTextField> textFields = new ArrayList<>();
    private int rangeaddress;

    public Master() throws IOException, ParseException {
        super("Master");
        this.rangeaddress = 0; // pocetna vrijednost adresnog raspona

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader("modbusSlaves.json"));
        JSONObject root = (JSONObject) obj; //cijeli dokument u JSONobjektu root

        JSONArray arr = (JSONArray) root.get("serverData");

        super.setLayout(new GridLayout(0, 2)); //postavljanje grida na frame

        for (int i = 0; i < arr.size(); ++i) {
            JSONObject slave = (JSONObject) arr.get(i); //svaki element niza je objekt, tj. slave

            String ip = (String) slave.get("ip");
            InetAddress addr = InetAddress.getByName(ip);
            long port = (long) slave.get("port");
            long unitID = (long) slave.get("unitID");

            JPanel panel = new JPanel(); //panel u koji stavljamo tablicu, labelu, textbox i button
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.setBorder(new EmptyBorder(10, 20, 10, 20)); //padding

            JLabel label = new JLabel("IP: " + ip + "      Port: " + port + "     UnitID: " + unitID);

            Object[] columnNames = {"Address", "Value"}; //header
            Object[][] data = {{"", ""}, {"", ""}, {"", ""}, {"", ""}, {"", ""}, {"", ""}, {"", ""}}; //tablica od 7 redaka
            JTable table = new JTable(data, columnNames);
            table.setName(Integer.toString(this.tables.size())); //tablici dajemo ime indexsa na kojem se nalazi u listi

            table.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent ke) {
                    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                        
                        //tabela ima ime indexsa na kojem se nalazi, tako cemo znati o kojoj se tabeli radi
                        int index = Integer.parseInt(table.getName());
                        Connection tempC = C.get(index);

                        String SelectedValue = (String) table.getValueAt(table.getSelectedRow()-1, table.getSelectedColumn());
                        
                        //tempC.setRegister(Integer.getInteger(SelectedValue));
                        //System.out.println(SelectedValue);
                    }
                }
            });

            this.tables.add(table); //referenca na svaku tablicu sa podacima

            JScrollPane scrollPane = new JScrollPane(table); //ako je tablica vecih dimencija onda je skrolamo i sluzi za prikaz headera

            table.setFillsViewportHeight(true);

            try {
                C.add(new Connection(ip, port, unitID)); //referenca na svaku konekciju nalazi se u listi sa konekcijama
            } catch (Exception ex) {
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }

            //text field za unos adresnog raspona, raspon je uvijek od 7 elemenata, ovdje unosimo pocetni element
            JTextField fromTField = new JTextField();

            //button za potvrdu unosa adresnog range-a
            JButton setRangeBtn = new JButton();

            setRangeBtn.setText(
                    "Set Range");

            /*dodjeljujemo mu ime ovisno koji ce se nalaziti u listi sa referencam ana buttonse
            to je kljucno poslije u action listeneru jer taj naziv je index preko kojega komuniciramo
            sa listom konekcija i listom texfield-ova*/
            setRangeBtn.setName(Integer.toString(this.buttons.size()));

            /*action listener za button, na temelju njegovog imena koje je index njegeovog
            mjesta u listi, kupio vrijednost TextFielda sa istim indexom u listi
            i mijenjamo range u konekciji koja je na istom indexu u listi konekcija
             */
            setRangeBtn.addActionListener(
                    new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e
                ) {
                    int index = Integer.parseInt(setRangeBtn.getName());
                    JTextField tempTextField = textFields.get(index);
                    Connection tempC = C.get(index);

                    rangeaddress = Integer.parseInt(tempTextField.getText());

                    tempC.setAddressRange(rangeaddress);
                }
            }
            );

            //ubacivamo button u listu sa referencom na sve buttons-e
            this.buttons.add(setRangeBtn);

            //panel u kojeg stavljamo button i From TextField radi lipseg prikaza
            JPanel texbtnPanel = new JPanel(new GridBagLayout());

            //dimenzija, background i poravnanje teksta za FromTextField
            fromTField.setPreferredSize(
                    new Dimension(100, 24));
            fromTField.setBackground(Color.LIGHT_GRAY);

            fromTField.setHorizontalAlignment(SwingConstants.RIGHT);

            //ubacivamo i FromTextField u listu sa referencama na sve FromTextFieldove
            this.textFields.add(fromTField);

            /*u postavljamo button i textfield u panel kojega poslije postavljamo u glavni panel
            sve radi prikaza*/
            texbtnPanel.add(fromTField);

            texbtnPanel.add(setRangeBtn);

            panel.add(label); //ip, port, unitID

            panel.add(scrollPane); //table

            panel.add(texbtnPanel); //FromTextField i button

            super.add(panel); //cijeli panel dodamo u frame

        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                callExecute(); //svako 5 sekundi mijenjamo vrijednosti
            }
        }, 0, 5000);
        //timer.cancel();

        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setSize(800, 700);
        super.setResizable(true);
        super.setVisible(true);
    }

    public void callExecute() {
        int tableNum = 0; //broji tablice u listi tablica
        for (Connection cnct : C) { //sve konekcije
            int row = 0;
            Map<Integer, Short> values = new HashMap<>();
            values = cnct.execute(); //vraca hashmapu sa parom adresa-value

            //iteracija kroz hashmapu za dohvacanje para key-value
            for (Entry<Integer, Short> entry : values.entrySet()) {
                //System.out.println(entry.getKey() + " : " + entry.getValue() + " ==> " + tableNum);
                String address = entry.getKey().toString();
                String value = entry.getValue().toString();
                tables.get(tableNum).getModel().setValueAt(address, row, 0);
                tables.get(tableNum).getModel().setValueAt(value, row, 1);
                row++; //punimo red i uvecamo ga da gleda na sljedeci
            }
            tableNum++;
        }
    }

    private static Master M;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    M = new Master();
                } catch (IOException ex) {
                    Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}

