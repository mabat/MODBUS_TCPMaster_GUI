package modbus_tcpmaster_gui;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteMultipleRegistersRequest;
import net.wimpi.modbus.msg.WriteMultipleRegistersResponse;
import net.wimpi.modbus.net.TCPMasterConnection;

public class Connection {

    private final String ip;
    private final long port;
    private final long unitID;
    private TCPMasterConnection con = null; //the connection
    private ModbusTCPTransaction trans = null; //the transaction
    private ReadMultipleRegistersRequest req = null; //the request
    private ReadMultipleRegistersResponse res = null; //the response
    private WriteMultipleRegistersRequest writeReq = null;
    private WriteMultipleRegistersResponse writeRes = null;
    private List<Integer> addressOfRegs = new ArrayList<>();
    private int addressRange;

    Connection(String ip, long port, long ID) throws UnknownHostException, Exception {

        //pocetna vrijednost raspona adresa 0-6
        for (int i = 0; i < 7; ++i) {
            this.addressOfRegs.add(i);
        }

        this.ip = ip;
        this.port = port;
        this.unitID = ID;
        InetAddress addr = InetAddress.getByName(this.ip);

        //spajanje
        con = new TCPMasterConnection(addr);
        con.setPort((int) port);
        con.connect();
    }

    //read register
    public Map<Integer, Short> execute() {

        Map<Integer, Short> Values = new HashMap<>(); //par adresa-value punimo u hashmapu i vracamo je

        for (int adr : this.addressOfRegs) { //raspon adresa

            req = new ReadMultipleRegistersRequest(adr, 1); //cita value sa adrese

            trans = new ModbusTCPTransaction(con);
            trans.setRequest(req);
            try {
                trans.execute();
            } catch (ModbusSlaveException ex) {
                JOptionPane.showMessageDialog(null, ex, "Modbus Slave Exception", JOptionPane.ERROR_MESSAGE);
            } catch (ModbusException ex) {
                JOptionPane.showMessageDialog(null, ex, "Modbus Exception", JOptionPane.ERROR_MESSAGE);
            }
            res = (ReadMultipleRegistersResponse) trans.getResponse();
            //return hashmap key value
            Values.put(adr, (short) res.getRegister(0).toUnsignedShort());
        }
        return Values;
    }

    public void setRegister(int selectedValue) {

        writeReq = new WriteMultipleRegistersRequest();
        trans = new ModbusTCPTransaction(con);
        trans.setRequest(writeReq);
        try {
            trans.execute();
        } catch (ModbusSlaveException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ModbusException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        writeRes = (WriteMultipleRegistersResponse) trans.getResponse();
    }

    public void setAddressRange(int range) {

        //postavljanje raspona adrese od dobivenog rangea + 6 vrijednosti
        addressOfRegs.removeAll(addressOfRegs);
        for (int i = range; i <= range + 6; ++i) { //maksimalno 7 vrijednosti
            addressOfRegs.add(i);
        }
    }

    public void closeConnection() {
        con.close();
    }

}

