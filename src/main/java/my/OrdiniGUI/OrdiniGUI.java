/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package my.OrdiniGUI;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import javax.swing.JOptionPane;
import java.sql.*;
import java.text.*;
import java.util.Calendar;
import javax.swing.JTable;
import net.proteanit.sql.DbUtils;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import my.OrdiniGUI.Gmail.Credentials;
import my.OrdiniGUI.Gmail.TestoEmail;
import org.apache.logging.log4j.LogManager;



/**
 *
 * @author Federico
 */
public class OrdiniGUI extends javax.swing.JFrame {

    
    private PreparedStatement Statement;
    private Connection conn = null;
    private ResultSet rs;
    private String CC = null;
    private String Spedizione;
    private String SpedizionePagata;
    private DefaultTableModel dm;
    private String dbPath;
    private String MPagamento;
    private Gmail service;
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(OrdiniGUI.class);
    
    /**
     * Creates new form OrdiniGUI
     */
    public OrdiniGUI(){
        initComponents();
        icona();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        //Ricerca path del programma
        File check_path = null;
        String jarDir = null;
        try {
            File temp = new File(OrdiniGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            jarDir = temp.getParentFile().getPath();
            check_path = new File(jarDir+"\\dbPath.txt");
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
            logger.error("Errore nel metodo principale", ex);
        }
        if(check_path.exists() && !check_path.isDirectory()) { 
            try {
                dbPath = new Scanner(new File(check_path.getAbsolutePath())).useDelimiter("\\A").next();
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(null, ex);
            }
        //controllo esistenza db, altrimenti creazione o richiesta del database
        } 
        if(!check_path.exists()) { 
            String[] options = new String[2];
            options[0] = "Crea Nuovo";
            options[1] = "Scegli";
            int opt = JOptionPane.showOptionDialog(null,"Nessun database presente. Vuoi sceglierne uno o crearne uno nuovo?","Database", 0,JOptionPane.PLAIN_MESSAGE,null,options,null);
            
            if(opt == 0){
                File database = new File(jarDir+"\\Ordini.db");
                dbPath = database.getAbsolutePath();
                createNewDatabase(dbPath);
            }else{
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("db File", "db");
                chooser.setFileFilter(filter);
                chooser.setCurrentDirectory(new File("."));
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.showOpenDialog(null);
                File selectedPfile = chooser.getSelectedFile();
                if(!(selectedPfile== null)){
                    dbPath = selectedPfile.getAbsolutePath().replace("\\" , "/");
                }else{
                    File database = new File(jarDir+"\\Ordini.db");
                    dbPath = database.getAbsolutePath();
                    createNewDatabase(dbPath);
                }
            } 
        }
        createFilePath(jarDir);
        update_table();
        populate_table_spedizioni();
        populate_table_ritiri_oggi();
        populate_table_ritiri_domani();
        populate_table_evasi();
        populate_table_spedizioniEvase();
        populate_table_ModificaMultipla();
        enablerClick();
        
        
        try {
            service = Credentials.getGmailService();
        } catch (Throwable  ex) {
            ex.printStackTrace();
            logger.error("Errore nel metodo principale", ex);
        }
        
        
    }
    
    private void icona(){
       this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/clienti.png")));
       Impostazioni.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/database.png")));
       Pannello.setIconAt(0, new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/scritto.png"))));
       Pannello.setIconAt(1, new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/utentiMultipli.png"))));
       Pannello.setIconAt(2, new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/nuclear.png"))));
       Pannello.setIconAt(3, new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/red-alert.png"))));
       Pannello.setIconAt(4, new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/lente.png"))));
       Pannello.setIconAt(5, new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/spedizione.png"))));
       Pannello.setIconAt(6, new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/evasi.png"))));
       Pannello.setIconAt(7, new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/immagini/spedizioniEvase.png"))));
    }
    
    private void enablerFromAction(){
        if(Sped.isSelected()){
            PagSped.setEnabled(true);
            cb_mpag.setEnabled(true);
            Indirizzo.setEnabled(true);
            Citta.setEnabled(true);
            Provincia.setEnabled(true);
            Tracking.setEnabled(true);
            CAP.setEnabled(true);
        }else{
            PagSped.setEnabled(false);
            cb_mpag.setEnabled(false);
            Indirizzo.setEnabled(false);
            Citta.setEnabled(false);
            Provincia.setEnabled(false);
            Tracking.setEnabled(false);
            CAP.setEnabled(false);
            }
        
    }
    
    private void enablerClick(){
            Sped.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (Sped.isSelected()){
                    PagSped.setEnabled(true);
                    cb_mpag.setEnabled(true);
                    Indirizzo.setEnabled(true);
                    Citta.setEnabled(true);
                    Provincia.setEnabled(true);
                    Tracking.setEnabled(true);
                    CAP.setEnabled(true);
                }else{
                    PagSped.setEnabled(false);
                    cb_mpag.setEnabled(false);
                    Indirizzo.setEnabled(false);
                    Citta.setEnabled(false);
                    Provincia.setEnabled(false);
                    Tracking.setEnabled(false);
                    CAP.setEnabled(false);
                }
            }
        });
        
    }
    
    public void createFilePath(String path){
        try {
                PrintStream out = new PrintStream(new FileOutputStream(path +"\\dbPath.txt"));
                out.print(dbPath);
                out.close();

      }catch(IOException i) {
         i.printStackTrace();
      
      }
    }
    public void createNewDatabase(String fileName) {
        try{
                String sql1 = "CREATE TABLE ordini " +
                              "(CodiceCliente VARCHAR(10) not NULL, " +
                              " Nome VARCHAR(45), " + 
                              " Cognome VARCHAR(45), " + 
                              " Telefono1 VARCHAR(45), " + 
                              " Telefono2 VARCHAR(45), " + 
                              " Email VARCHAR(45), " + 
                              " dRitiro VARCHAR(45), " + 
                              " LibriTrovati VARCHAR(45), " + 
                              " Spedizione VARCHAR(4), " + 
                              " SpedPagata VARCHAR(4), " + 
                              " Indirizzo VARCHAR(45), " + 
                              " CAP VARCHAR(10), " + 
                              " Citta VARCHAR(20), " + 
                              " Provincia VARCHAR(4), " + 
                              " MPagamento VARCHAR(20), " + 
                              " Tracking VARCHAR(30), " + 
                              " Note VARCHAR(3000), " + 
                              " Spacchettato VARCHAR(5), " + 
                              " Ritirato VARCHAR(5), " + 
                              " PRIMARY KEY ( CodiceCliente ))";
                String sql2 = "CREATE TABLE email " +
                              "(EmailConTelefono VARCHAR(5000) , " +
                              " EmailSenzaTelefono VARCHAR(5000) , " +
                              " EmailNoLibri VARCHAR(5000)) ";
                String sql3 = "INSERT INTO email (EmailConTelefono,EmailSenzaTelefono,EmailNoLibri) values ('','','')";
                conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
                Statement = conn.prepareStatement(sql1);
                Statement.execute();
                Statement.close();
                Statement = conn.prepareStatement(sql2);
                Statement.execute();
                Statement.close();
                Statement = conn.prepareStatement(sql3);
                Statement.execute();
                Statement.close();
                conn.close();
                JOptionPane.showMessageDialog(null, "Un nuovo database è stato creato", "Creato",JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e, "Errore 2", JOptionPane.PLAIN_MESSAGE);
            logger.error("Errore in createNewDatabase", e);
        }
    }
   
    
    private void cancellaDb(){
        try {
            String sql1 = "DELETE FROM ordini";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql1);
            Statement.execute();
            Statement.close();
            conn.close();
            JOptionPane.showMessageDialog(null, "Database cancellato", "Cancellato",JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            logger.error("Errore in cancellaDb", ex);
        }
    }
    
    private void filter(String query, JTable table){
        dm = (DefaultTableModel) table.getModel();
        TableRowSorter<DefaultTableModel> tr = new TableRowSorter<DefaultTableModel>(dm);
        table.setRowSorter(tr);
        tr.setRowFilter(RowFilter.regexFilter("(?i)" + query));
    }
    
    private void modificaDati(JTable table){
        try{
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            String Table_Click = (table.getModel().getValueAt(row, 0).toString());
            String sql = "select * from ordini where CodiceCliente='"+Table_Click+"'";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            ResultSet rs = Statement.executeQuery();
            if(rs.next()){
                String add1 = rs.getString("CodiceCliente");
                CodiceCliente.setText(add1);
                CC = add1;

                String add2 = rs.getString("Nome");
                Nome.setText(add2);

                String add3 = rs.getString("Cognome");
                Cognome.setText(add3);

                String add4 = rs.getString("Telefono1");
                Tel1.setText(add4);

                String add5 = rs.getString("Telefono2");
                Tel2.setText(add5);

                String add6 = rs.getString("Email");
                Email.setText(add6);

                String add7 = rs.getString("dRitiro");
                DataRitiro.setText(add7);

                String add12 = rs.getString("Indirizzo");
                Indirizzo.setText(add12);

                String add13 = rs.getString("CAP");
                CAP.setText(add13);

                String add14 = rs.getString("Citta");
                Citta.setText(add14);

                String add15 = rs.getString("Provincia");
                Provincia.setText(add15);

                String add16 = rs.getString("LibriTrovati");
                libriTrovati.setText(add16);

                String add8 = rs.getString("Spedizione");
                String add9 = rs.getString("SpedPagata");
                String add10 = rs.getString("MPagamento");
                String add11 = rs.getString("Tracking");
                String add21 = rs.getString("Ritirato");
                String add22 = rs.getString("Spacchettato");
                Tracking.setText(add11);
                String add20 = rs.getString("Note");
                note.setText(add20);
                if(!(add8 == null)){
                    //Imposta Check box Spedizione
                    if(add8.compareTo("SI") == 0){
                        Sped.setSelected(true);

                        //Imposta ComboBox
                        if(add10 == null || add10.compareTo("") == 0){
                            cb_mpag.setSelectedIndex(-1);
                        }else
                            if(add10.compareTo("PayPal") == 0){
                                cb_mpag.setSelectedIndex(0);
                            } else
                                cb_mpag.setSelectedIndex(1);


                        //Imposta Check Box Spedizione Pagata
                        if(add9.compareTo("SI") == 0){
                            PagSped.setSelected(true);
                        } else
                            PagSped.setSelected(false);
                    } else{
                        Sped.setSelected(false);
                        PagSped.setSelected(false);
                        cb_mpag.setSelectedIndex(-1);
                    }
                } else{
                    Sped.setSelected(false);
                    PagSped.setSelected(false);
                    cb_mpag.setSelectedIndex(-1);
                }
                
                if(add21 != null){
                    if(add21.compareTo("SI") == 0)
                        ritirato.setSelected(true);
                }
                if(add22 != null){
                    if(add22.compareTo("SI") == 0)
                        spacchettato.setSelected(true);
                }

            }
            Statement.close();
            conn.close();
            Pannello.setSelectedIndex(0);
            }catch(SQLException e){
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, e, "Errore modificaDati()", JOptionPane.ERROR_MESSAGE);
                logger.error("Errore in modificaDati", e);
            }
    }
    
    private void salva(){
        if(!(CodiceCliente.getText().equals(""))){
            try{
                String sql = "Insert into ordini (CodiceCliente,Nome,Cognome,Telefono1,Telefono2,Email,dRitiro,Spedizione,SpedPagata,Indirizzo,CAP,Citta,Provincia,MPagamento,Tracking,Note,LibriTrovati,Spacchettato,Ritirato) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement = conn.prepareStatement(sql);
                ValoriSpedizione();
                String Ritirato = null;
                String Spacchettato = null;
                if(ritirato.isSelected()){
                    Ritirato = "SI";
                }else 
                    Ritirato = "NO";
                if(spacchettato.isSelected()){
                    Spacchettato = "SI";
                }else 
                    Spacchettato = "NO";
                
                Statement.setString(1, CodiceCliente.getText());
                Statement.setString(2, Nome.getText());
                Statement.setString(3, Cognome.getText());
                Statement.setString(4, Tel1.getText());
                Statement.setString(5, Tel2.getText());
                Statement.setString(6, Email.getText());
                Statement.setString(7,DataRitiro.getText());
                Statement.setString(8, Spedizione);
                Statement.setString(9, SpedizionePagata);
                Statement.setString(10, Indirizzo.getText());
                Statement.setString(11, CAP.getText());
                Statement.setString(12, Citta.getText());
                Statement.setString(13, Provincia.getText());
                Statement.setString(14, MPagamento);
                Statement.setString(15, Tracking.getText());
                Statement.setString(16, note.getText());
                Statement.setString(17, libriTrovati.getText());
                Statement.setString(18, Spacchettato);
                Statement.setString(19, Ritirato);

                Statement.execute();
                Statement.close();
                conn.close();
                
                this.Spedizione = null;
                this.SpedizionePagata = null;
                this.MPagamento = null;
                
                //Se abbiamo l'email allora entriamo dentro il metodo
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                
                JTextPane pane = new JTextPane();
                pane.setContentType("text/html");
                
                
                if(Email.getText() != null && !Email.getText().equals("")){
                    //Caso 1: Abbiamo il telefono
                    if((Tel1.getText() != null && !Tel1.getText().equals("")) || (Tel2.getText() != null && !Tel2.getText().equals(""))){
                        
                        sql = "select EmailConTelefono from email";
                        Statement = conn.prepareStatement(sql);
                        rs = Statement.executeQuery();
                        String testoEmail = rs.getString("EmailConTelefono");
                        
                        pane.setText(testoEmail);
                        
                        if(testoEmail != null && !testoEmail.equals("") && pane.getDocument().getLength()-1 != 0){
                            testoEmail = testoEmail.format(testoEmail,CodiceCliente.getText());

                            MimeMessage email = TestoEmail.createEmail(Email.getText(), "me", "Grazie per il suo ordine!", testoEmail);
                            TestoEmail.sendMessage(service, "me", email);
                        } else{
                            JOptionPane.showMessageDialog(null, "Nessun testo Email inserito. Inserire un testo nelle impostazioni e riprovare.", "Errore", JOptionPane.ERROR_MESSAGE);
                        }
                    } 
                    //Caso 2: Non abbiamo il telefono
                    else{

                        sql = "select EmailSenzaTelefono from email";
                        Statement = conn.prepareStatement(sql);
                        rs = Statement.executeQuery();
                        String testoEmail = rs.getString("EmailSenzaTelefono");
                        
                        pane.setText(testoEmail);
                        
                        if(testoEmail != null && !testoEmail.equals("") && pane.getDocument().getLength()-1 != 0){
                            testoEmail = testoEmail.format(testoEmail,CodiceCliente.getText());

                            MimeMessage email = TestoEmail.createEmail(Email.getText(), "me", "Grazie per il suo ordine!", testoEmail);
                            TestoEmail.sendMessage(service, "me", email);
                        } else{
                            JOptionPane.showMessageDialog(null, "Nessun testo Email inserito. Inserire un testo nelle impostazioni e riprovare.", "Errore", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    
                }
                
                pane = null;
                Statement.close();
                conn.close();
                
                update_table();
                
                DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                Calendar cal = Calendar.getInstance();
                info.setText(dateFormat.format(cal.getTime())+": Codice Cliente "+ CodiceCliente.getText() +" creato.");
                enablerFromAction();
                clear_fields();
                
                //Imposto la selezione sull'ultima riga e scrollo li
                int lastRow = get_clienti.convertRowIndexToView(get_clienti.getModel().getRowCount() - 1);
                get_clienti.setRowSelectionInterval(lastRow, lastRow);
                get_clienti.scrollRectToVisible(get_clienti.getCellRect(get_clienti.getRowCount()-1, 0, true));

            }catch(SQLException e){
                if (e.getErrorCode() == 19){
                    JOptionPane.showMessageDialog(null, "Codice Cliente già esistente", "Errore", JOptionPane.ERROR_MESSAGE);
                }else{
                JOptionPane.showMessageDialog(null, e);
                logger.error("Errore in salva", e);
                }
            } catch( MessagingException | IOException ex){
                JOptionPane.showMessageDialog(null, ex);
                logger.error("Errore in salva", ex);
            }
        }else{
            JOptionPane.showMessageDialog(null, "Nessun cliente selezionato", "Errore", JOptionPane.ERROR_MESSAGE);
            
        }
    }
    
    private void aggiornaTabelle(){
        update_table();
        populate_table_spedizioni();
        populate_table_ritiri_oggi();
        populate_table_ritiri_domani();
        populate_table_evasi();
        populate_table_spedizioniEvase();
        populate_table_ModificaMultipla();
    }
    
    private void populate_table_ritiri_domani(){
        
        try{
            DateFormat dateFormat = new SimpleDateFormat("dd/MM");
            Date domani = new Date();
            domani.setDate(domani.getDate() + 1);
            String sql = "select CodiceCliente, Nome, Cognome, Email, LibriTrovati from ordini where dRitiro='"+dateFormat.format(domani)+"' AND Ritirato = 'NO' AND Spacchettato = 'NO' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            tbl_ritiridomani.setModel(DbUtils.resultSetToTableModel(rs));
            Statement.close();
            conn.close();
        }catch (SQLException e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in populate_table_ritiri_domani", e);
        
        }
        
    }
        
    private void populate_table_ritiri_oggi(){ 
        try{
            DateFormat dateFormat = new SimpleDateFormat("dd/MM");
            Date oggi = new Date();
            String sql = "select CodiceCliente, Nome, Cognome, Email, LibriTrovati from ordini where dRitiro='"+dateFormat.format(oggi)+"' AND Ritirato = 'NO' AND Spacchettato = 'NO' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            tbl_ritirioggi.setModel(DbUtils.resultSetToTableModel(rs));
            Statement.close();
            conn.close();
        }catch (SQLException e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in populate_table_ritiri_oggi", e);
        }
        
    }
    
    private void populate_table_spedizioni(){

        try{
            String sql = "select * from ordini where Spedizione = 'SI' AND Ritirato = 'NO' AND Spacchettato = 'NO' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            tbl_spedizioni.setModel(DbUtils.resultSetToTableModel(rs));
            Statement.close();
            conn.close();
        }catch(SQLException e){

            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in populate_table_spedizioni", e);
        }
    }
    
    private void populate_table_evasi(){

        try{
            String sql = "SELECT * FROM ordini WHERE Spacchettato = 'SI' OR Ritirato = 'SI' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            tbl_evasi.setModel(DbUtils.resultSetToTableModel(rs));
            Statement.close();
            conn.close();
        }catch(SQLException e){

            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in populate_table_evasi", e);
        }
    }
        
    private void populate_table_spedizioniEvase(){

        try{
            String sql = "SELECT * FROM ordini WHERE Spedizione = 'SI' AND Spacchettato = 'SI' OR Spedizione = 'SI' AND Ritirato = 'SI' OR Spedizione = 'SI' AND SpedPagata = 'SI' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            tbl_spedizioniEvase.setModel(DbUtils.resultSetToTableModel(rs));
            Statement.close();
            conn.close();
        }catch(SQLException e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in populate_table_spedizioniEvase", e);
        }
    }
    
        private void populate_table_ModificaMultipla(){

        try{
            String sql = "select * from ordini WHERE Ritirato = 'NO' AND Spacchettato ='NO' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            
            TableRowSorter sorter = (TableRowSorter)tblModificaMultipla.getRowSorter();
            List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();
            
            tblModificaMultipla.setModel(DbUtils.resultSetToTableModel(rs));
            
            sorter = (TableRowSorter)tblModificaMultipla.getRowSorter();
            sorter.setSortKeys(sortKeys);
            Statement.close();
            conn.close();
        }catch(SQLException e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in populate_table_ModificaMultipla", e);
        }
    }
        
    private void update_table(){
        try{
            String sql = "select * from ordini WHERE Ritirato = 'NO' AND Spacchettato ='NO' AND SpedPagata = '' OR Ritirato = 'NO' AND Spacchettato ='NO' AND SpedPagata = 'NO' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            
            //Mi salvo l'ordine in come sono salvate le colonne
            TableRowSorter sorter = (TableRowSorter)get_clienti.getRowSorter();
            List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();
            int sRow = get_clienti.getSelectedRow();
            
            //Imposto il model aggiornato
            get_clienti.setModel(DbUtils.resultSetToTableModel(rs));
            
            //Re-imposto l'ordine delle colonne
            sorter = (TableRowSorter)get_clienti.getRowSorter();
            sorter.setSortKeys(sortKeys);
            
            //Controllo se avevo una riga selezionata, se si allora la riseleziono cosi' da poter usare le freccette
            if(sRow >= 0 && get_clienti.getRowCount() != 0 && get_clienti.getRowCount() > sRow){
                get_clienti.setRowSelectionInterval(sRow, sRow);
                get_clienti.requestFocus();
            }
            int columns = get_clienti.getColumnCount();
            
            //imposto la grandezza delle varie colonne; La prima deve solo essere a "filo" con il codice
            get_clienti.getColumnModel().getColumn(0).setPreferredWidth(55);
            for(int i = 1; i<columns; i++){
                get_clienti.getColumnModel().getColumn(i).setPreferredWidth(115);
            }
            
            Statement.close();
            conn.close();
        }catch (SQLException e){
            JOptionPane.showMessageDialog(null, e);
            e.printStackTrace();
            logger.error("Errore in update_table", e);
        
        }
    }
    
    public void clear_fields(){
         CodiceCliente.setText("");
           Nome.setText("");
           Cognome.setText("");
           Tel1.setText("");
           Tel2.setText("");
           Email.setText("");
           DataRitiro.setText("");
           search_for.setText("");
           Indirizzo.setText("");
           CAP.setText("");
           Citta.setText("");
           Provincia.setText("");
           Tracking.setText("");
           note.setText("");
           libriTrovati.setText("");
           Sped.setSelected(false);
           PagSped.setSelected(false);
           ritirato.setSelected(false);
           spacchettato.setSelected(false);
           cb_mpag.setSelectedIndex(-1);
           PagSped.setEnabled(false);
           cb_mpag.setEnabled(false);
           Indirizzo.setEnabled(false);
           Citta.setEnabled(false);
           Provincia.setEnabled(false);
           Tracking.setEnabled(false);
           CAP.setEnabled(false);
           
           this.CC = null;
    }
    
    public void ValoriSpedizione(){
        
        //Check Box "Spedizione?"
        if (Sped.isSelected()){
            Spedizione = "SI";
            
            //Check Box "Spedizione Pagata"
            if (PagSped.isSelected()){
            SpedizionePagata = "SI";
            } else {
            SpedizionePagata = "NO";   
            }
            
            //selezione del Combo Box
            if(cb_mpag.isEnabled()){
                if(cb_mpag.getSelectedIndex() == 0)
                    MPagamento = "PayPal";
                if(cb_mpag.getSelectedIndex() == 1)
                    MPagamento = "Bonifico";
            }else{
                MPagamento = "";
            }
        } else {
            Spedizione = "";
            SpedizionePagata = "";   
            MPagamento = "";
        }
    }
    
    private void aggiorna(){
        if(!(CodiceCliente.getText().equals(""))){
            try{
                String val0 = CodiceCliente.getText().replace("'","''");
                String val1 = Nome.getText().replace("'","''");
                String val2 = Cognome.getText().replace("'","''");
                String val3 = Tel1.getText();
                String val4 = Tel2.getText();
                String val5 = Email.getText();
                String val6 = DataRitiro.getText();
                String val7 = Indirizzo.getText().replace("'","''");
                String val8 = CAP.getText();
                String val9 = Citta.getText().replace("'","''");
                String val10 = Provincia.getText().replace("'","''");
                String val11 = Tracking.getText().replace("'","''");
                String val12 = note.getText().replace("'","''");
                String val13 = libriTrovati.getText();
                
                String Ritirato = null;
                String Spacchettato = null;
                if(ritirato.isSelected()){
                    Ritirato = "SI";
                }else 
                    Ritirato = "NO";
                if(spacchettato.isSelected()){
                    Spacchettato = "SI";
                }else 
                    Spacchettato = "NO";
                
                ValoriSpedizione();
                
                //Controllo il numero dei libri trovati: Se 0 allora invio email e poi spacchetto
                if(val13.equals("0")){
                    conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                    String sql = "select EmailNoLibri from email";
                    Statement = conn.prepareStatement(sql);
                    rs = Statement.executeQuery();
                    String testoEmail = rs.getString("EmailNoLibri");
                    
                    JTextPane pane = new JTextPane();
                    pane.setContentType("text/html");
                    pane.setText(testoEmail);
                    
                    if(testoEmail != null && !testoEmail.equals("") && pane.getDocument().getLength()-1 != 0){
                        MimeMessage email = TestoEmail.createEmail(Email.getText(), "me", "Ordine libri Oberdan15", testoEmail);
                        TestoEmail.sendMessage(service, "me", email);
                        
                        Spacchettato = "SI";
                    } else{
                        JOptionPane.showMessageDialog(null, "Nessun testo Email inserito. Inserire un testo nelle impostazioni e riprovare.", "Errore", JOptionPane.ERROR_MESSAGE);
                    }
                    
                    pane = null;
                    rs.close();
                    Statement.close();
                    conn.close();
                }
                
                String sql = "update ordini set CodiceCliente='"+val0+"',Nome='"+val1+"',Cognome='"+val2+"',Telefono1='"+val3+"',Telefono2='"+val4+"',Email='"+val5+"',dRitiro='"+val6+"',Spedizione='"+Spedizione+"',SpedPagata='"+SpedizionePagata+"',Indirizzo='"+val7+"',CAP='"+val8+"',Citta='"+val9+"',Provincia='"+val10+"',MPagamento='"+MPagamento+"',Tracking='"+val11+"',Note='"+val12+"',LibriTrovati='"+val13+"',Spacchettato='"+Spacchettato+"',Ritirato='"+Ritirato+"' where CodiceCliente ='"+CC+"'";
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement = conn.prepareStatement(sql);
                Statement.execute();
                
                Spedizione = null;
                SpedizionePagata = null;
                MPagamento = null;
                this.CC = null;
                
                DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                Calendar cal = Calendar.getInstance();
                info.setText(dateFormat.format(cal.getTime())+": Codice Cliente "+ CodiceCliente.getText() +" modificato.");
                
                Statement.close();
                conn.close();
            }catch(Exception e){
                JOptionPane.showMessageDialog(null, e);
                logger.error("Errore in aggiorna", e);
            }
            update_table();
            clear_fields();
        }else{
            JOptionPane.showMessageDialog(null, "Nessun cliente selezionato", "Errore", JOptionPane.ERROR_MESSAGE);
            
        }     
    }
    
    private void elimina(){
        if(!(CodiceCliente.getText().equals(""))){
            int conf = JOptionPane.showConfirmDialog(null, "Eliminare il cliente?","Elimina",JOptionPane.YES_NO_OPTION);
            if(conf == 0){
                String sql = "delete from ordini where CodiceCliente = ?";

                try{
                    conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                    Statement= conn.prepareStatement(sql);
                    Statement.setString(1, CodiceCliente.getText());
                    Statement.execute();
                    Statement.close();
                    conn.close();


                }catch(SQLException e){
                    JOptionPane.showMessageDialog(null, e);
                    logger.error("Errore in elimina", e);
                }
                DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                Calendar cal = Calendar.getInstance();
                info.setText(dateFormat.format(cal.getTime())+": Codice Cliente "+ CodiceCliente.getText() +" eliminato.");
                
                get_clienti.clearSelection();
                update_table();
                clear_fields();
            }
        }else{
            JOptionPane.showMessageDialog(null, "Nessun cliente selezionato", "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void stampaOggi(){
        DateFormat dateFormat = new SimpleDateFormat("dd/MM");
        Date oggi = new Date();
        MessageFormat header = new MessageFormat("Ritiri "+dateFormat.format(oggi));
        try{

            tbl_ritirioggi.print(JTable.PrintMode.FIT_WIDTH, header, null);

        }catch(java.awt.print.PrinterException e){
            System.err.format("Impossibile Stampare %%n", e.getMessage());
            logger.error("Errore in stampaOggi", e);
        }
    }
    
    private void stampaDomani(){
        DateFormat dateFormat = new SimpleDateFormat("dd/MM");
        Date domani = new Date();
        domani.setDate(domani.getDate() + 1);
        MessageFormat header = new MessageFormat("Ritiri "+dateFormat.format(domani));
        try{

            tbl_ritiridomani.print(JTable.PrintMode.FIT_WIDTH, header, null);

        }catch(java.awt.print.PrinterException e){
            System.err.format("Impossibile Stampare %%n", e.getMessage());
            logger.error("Errore in stampaDomani", e);
        }
    }
    
    private void stampaInserimento(){
        MessageFormat header = new MessageFormat("Lista Ordini");
        MessageFormat footer = new MessageFormat("Pagina {0, number, integer}");
        try{

            get_clienti.print(JTable.PrintMode.FIT_WIDTH, header, footer);

        }catch(java.awt.print.PrinterException e){
            System.err.format("Impossibile Stampare %%n", e.getMessage());
            logger.error("Errore in stampaInserimento", e);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Impostazioni = new javax.swing.JFrame();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        pathDb = new javax.swing.JTextField();
        btn_impDb = new javax.swing.JButton();
        btn_salvaDb = new javax.swing.JButton();
        btn_closeImp = new javax.swing.JButton();
        btn_nuovoDb = new javax.swing.JButton();
        jLabel22 = new javax.swing.JLabel();
        dbCorrente = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        cbEmailImpostazioni = new javax.swing.JComboBox<>();
        lblEmailImpostazioni = new javax.swing.JLabel();
        visualizzaEmailImpostazioni = new javax.swing.JButton();
        salvaEmailImpostazioni = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jScrollPane10 = new javax.swing.JScrollPane();
        epEmailImpostazioni = new javax.swing.JTextPane();
        InvioEmail = new javax.swing.JFrame();
        jPanel10 = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        destinatario = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        oggetto = new javax.swing.JTextField();
        invioEmail = new javax.swing.JButton();
        jScrollPane11 = new javax.swing.JScrollPane();
        testo = new javax.swing.JEditorPane();
        Pannello = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        get_clienti = new javax.swing.JTable();
        Indirizzo = new javax.swing.JTextField();
        CAP = new javax.swing.JTextField();
        Citta = new javax.swing.JTextField();
        Provincia = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        CodiceCliente = new javax.swing.JTextField();
        Nome = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        Email = new javax.swing.JTextField();
        Tel2 = new javax.swing.JTextField();
        Tel1 = new javax.swing.JTextField();
        Cognome = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        DataRitiro = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        search_for = new javax.swing.JTextField();
        Sped = new javax.swing.JCheckBox();
        PagSped = new javax.swing.JCheckBox();
        btn_salva = new javax.swing.JButton();
        btn_aggiorna = new javax.swing.JButton();
        btn_cancella = new javax.swing.JButton();
        btn_clear = new javax.swing.JButton();
        btn_stampa = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        cb_mpag = new javax.swing.JComboBox<>();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        Tracking = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        btn_refresh = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        note = new javax.swing.JTextArea();
        info = new javax.swing.JLabel();
        libriTrovati = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        ritirato = new javax.swing.JCheckBox();
        spacchettato = new javax.swing.JCheckBox();
        emailTemp = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        ritirato1 = new javax.swing.JCheckBox();
        spacchettato1 = new javax.swing.JCheckBox();
        btnModificaMultipla = new javax.swing.JButton();
        jScrollPane7 = new javax.swing.JScrollPane();
        tblModificaMultipla = new javax.swing.JTable();
        btnModificaModificaMultipla = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tbl_ritirioggi = new javax.swing.JTable();
        btn_ritirioggi = new javax.swing.JButton();
        btn_stampaoggi = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        btn_ritiridomani = new javax.swing.JButton();
        btn_stampadomani = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tbl_ritiridomani = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tbl_ritirigiorno = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        giornoRitiro = new javax.swing.JTextField();
        btn_ritiriPerGiorno = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        btn_spedizioni = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        tbl_spedizioni = new javax.swing.JTable();
        jPanel7 = new javax.swing.JPanel();
        btn_evasi = new javax.swing.JButton();
        jScrollPane8 = new javax.swing.JScrollPane();
        tbl_evasi = new javax.swing.JTable();
        jLabel24 = new javax.swing.JLabel();
        cercaEvasi = new javax.swing.JTextField();
        jPanel8 = new javax.swing.JPanel();
        btn_spedizioniEvase = new javax.swing.JButton();
        jScrollPane9 = new javax.swing.JScrollPane();
        tbl_spedizioniEvase = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        file_menu = new javax.swing.JMenu();
        mn_salva = new javax.swing.JMenuItem();
        mn_aggiorna = new javax.swing.JMenuItem();
        mn_cancella = new javax.swing.JMenuItem();
        mn_elimina = new javax.swing.JMenuItem();
        mn_aggiornaTabella = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        mn_stampa = new javax.swing.JMenuItem();
        mn_stampaoggi = new javax.swing.JMenuItem();
        mn_stampadomani = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mn_impostazioni = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mn_esci = new javax.swing.JMenuItem();

        Impostazioni.setTitle("Impostazioni Database");
        Impostazioni.setMinimumSize(new java.awt.Dimension(489, 220));

        jPanel6.setMinimumSize(new java.awt.Dimension(489, 420));
        jPanel6.setPreferredSize(new java.awt.Dimension(489, 230));

        jLabel19.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel19.setText("IMPOSTAZIONI DATABASE CLIENTI");

        jLabel20.setText("Seleziona il Database:");

        btn_impDb.setText("Apri..");
        btn_impDb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_impDbActionPerformed(evt);
            }
        });

        btn_salvaDb.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/salva.png"))); // NOI18N
        btn_salvaDb.setText("Salva");
        btn_salvaDb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_salvaDbActionPerformed(evt);
            }
        });

        btn_closeImp.setText("Chiudi");
        btn_closeImp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_closeImpActionPerformed(evt);
            }
        });

        btn_nuovoDb.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/database.png"))); // NOI18N
        btn_nuovoDb.setText("Genera nuovo Database");
        btn_nuovoDb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_nuovoDbActionPerformed(evt);
            }
        });

        jLabel22.setText("Database attuale:");

        dbCorrente.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        dbCorrente.setText("aa");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(btn_salvaDb)
                        .addGap(18, 18, 18)
                        .addComponent(btn_nuovoDb)
                        .addGap(16, 16, 16)
                        .addComponent(btn_closeImp))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addComponent(jLabel22))
                        .addGap(6, 6, 6)
                        .addComponent(pathDb, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_impDb))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(dbCorrente))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel19)))
                .addContainerGap(280, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel19)
                .addGap(34, 34, 34)
                .addComponent(jLabel22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dbCorrente)
                .addGap(38, 38, 38)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(pathDb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_impDb))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_closeImp)
                    .addComponent(btn_salvaDb)
                    .addComponent(btn_nuovoDb))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Database", jPanel6);

        cbEmailImpostazioni.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Email con Telefono", "Email senza Telefono", "Nessun libro Trovato" }));

        lblEmailImpostazioni.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lblEmailImpostazioni.setText("IMPOSTAZIONI DATABASE CLIENTI");

        visualizzaEmailImpostazioni.setText("Visualizza");
        visualizzaEmailImpostazioni.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                visualizzaEmailImpostazioniActionPerformed(evt);
            }
        });

        salvaEmailImpostazioni.setText("Salva");
        salvaEmailImpostazioni.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salvaEmailImpostazioniActionPerformed(evt);
            }
        });

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/info.png"))); // NOI18N
        jButton1.setToolTipText("<html>\nPer inserire il codice cliente usare la stringa:<br><br>\n\n\"<b>%s</b>\" (senza virgolette)\n</html>");
        jButton1.setMaximumSize(new java.awt.Dimension(25, 25));
        jButton1.setMinimumSize(new java.awt.Dimension(25, 25));
        jButton1.setPreferredSize(new java.awt.Dimension(25, 25));

        epEmailImpostazioni.setContentType("text/html"); // NOI18N
        jScrollPane10.setViewportView(epEmailImpostazioni);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jScrollPane10)
                        .addContainerGap())
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(lblEmailImpostazioni)
                        .addContainerGap(389, Short.MAX_VALUE))
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(cbEmailImpostazioni, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(50, 50, 50)
                        .addComponent(visualizzaEmailImpostazioni)
                        .addGap(136, 136, 136)
                        .addComponent(salvaEmailImpostazioni)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 151, Short.MAX_VALUE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(89, 89, 89))))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblEmailImpostazioni)
                .addGap(18, 18, 18)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbEmailImpostazioni, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(visualizzaEmailImpostazioni)
                    .addComponent(salvaEmailImpostazioni)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Email", jPanel11);

        javax.swing.GroupLayout ImpostazioniLayout = new javax.swing.GroupLayout(Impostazioni.getContentPane());
        Impostazioni.getContentPane().setLayout(ImpostazioniLayout);
        ImpostazioniLayout.setHorizontalGroup(
            ImpostazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        ImpostazioniLayout.setVerticalGroup(
            ImpostazioniLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        InvioEmail.setTitle("Invio Email");
        InvioEmail.setMinimumSize(new java.awt.Dimension(916, 611));

        jLabel25.setText("Destinatario:");

        jLabel26.setText("Oggetto:");

        jLabel27.setText("Testo:");

        invioEmail.setText("Invia Email");
        invioEmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invioEmailActionPerformed(evt);
            }
        });

        testo.setContentType("text/html"); // NOI18N
        jScrollPane11.setViewportView(testo);

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel26)
                            .addComponent(jLabel27))
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel10Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(invioEmail))
                            .addGroup(jPanel10Layout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addComponent(jScrollPane11, javax.swing.GroupLayout.PREFERRED_SIZE, 754, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(oggetto, javax.swing.GroupLayout.PREFERRED_SIZE, 277, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(477, 477, 477))))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jLabel25)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(destinatario, javax.swing.GroupLayout.PREFERRED_SIZE, 277, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(86, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel25)
                    .addComponent(destinatario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel26)
                    .addComponent(oggetto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel27)
                    .addComponent(jScrollPane11, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(invioEmail)
                .addContainerGap(176, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout InvioEmailLayout = new javax.swing.GroupLayout(InvioEmail.getContentPane());
        InvioEmail.getContentPane().setLayout(InvioEmailLayout);
        InvioEmailLayout.setHorizontalGroup(
            InvioEmailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        InvioEmailLayout.setVerticalGroup(
            InvioEmailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Gestione Ordini");

        Pannello.setPreferredSize(new java.awt.Dimension(1224, 640));

        jScrollPane6.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane6.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        get_clienti.setAutoCreateRowSorter(true);
        get_clienti.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        get_clienti.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        )
    );
    get_clienti.setDefaultEditor(Object.class, null);
    get_clienti.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
    get_clienti.setRowHeight(25);
    get_clienti.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            get_clientiMouseClicked(evt);
        }
    });
    get_clienti.addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyReleased(java.awt.event.KeyEvent evt) {
            get_clientiKeyReleased(evt);
        }
    });
    jScrollPane6.setViewportView(get_clienti);

    Indirizzo.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    Indirizzo.setEnabled(false);

    CAP.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    CAP.setEnabled(false);

    Citta.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    Citta.setEnabled(false);

    Provincia.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    Provincia.setEnabled(false);

    jLabel15.setText("Provincia:");

    jLabel14.setText("Città:");

    jLabel12.setText("CAP:");

    jLabel13.setText("Indirizzo:");

    CodiceCliente.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

    Nome.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

    jLabel9.setText("Codice Cliente:");

    jLabel1.setText("Nome:");

    jLabel2.setText("Cognome:");

    jLabel3.setText("Telefono 1:");

    jLabel4.setText("Telefono 2:");

    jLabel5.setText("E-Mail:");

    Email.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

    Tel2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

    Tel1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

    Cognome.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

    jLabel7.setText("Data Ritiro:");

    DataRitiro.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    DataRitiro.setToolTipText("Formato data: gg/MM");

    jLabel10.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
    jLabel10.setText("Ricerca");

    jLabel11.setText("Cerca:");

    search_for.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    search_for.setToolTipText("Ricerca per qualsiasi dato. (Codice Cliente, Nome, Cognome, ecc.)");
    search_for.addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyReleased(java.awt.event.KeyEvent evt) {
            search_forKeyReleased(evt);
        }
    });

    Sped.setText("Spedizione?");

    PagSped.setText("Spedizione Pagata");
    PagSped.setEnabled(false);

    btn_salva.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/salva.png"))); // NOI18N
    btn_salva.setText("Salva");
    btn_salva.setMaximumSize(new java.awt.Dimension(103, 25));
    btn_salva.setMinimumSize(new java.awt.Dimension(103, 25));
    btn_salva.setPreferredSize(new java.awt.Dimension(103, 25));
    btn_salva.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_salvaActionPerformed(evt);
        }
    });

    btn_aggiorna.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/modifica.png"))); // NOI18N
    btn_aggiorna.setText("Modifica");
    btn_aggiorna.setMaximumSize(new java.awt.Dimension(103, 25));
    btn_aggiorna.setMinimumSize(new java.awt.Dimension(103, 25));
    btn_aggiorna.setPreferredSize(new java.awt.Dimension(103, 25));
    btn_aggiorna.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_aggiornaActionPerformed(evt);
        }
    });

    btn_cancella.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/elimina.png"))); // NOI18N
    btn_cancella.setText("Elimina");
    btn_cancella.setMaximumSize(new java.awt.Dimension(103, 25));
    btn_cancella.setMinimumSize(new java.awt.Dimension(103, 25));
    btn_cancella.setPreferredSize(new java.awt.Dimension(103, 25));
    btn_cancella.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_cancellaActionPerformed(evt);
        }
    });

    btn_clear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/cancella.png"))); // NOI18N
    btn_clear.setText("Cancella");
    btn_clear.setMaximumSize(new java.awt.Dimension(103, 25));
    btn_clear.setMinimumSize(new java.awt.Dimension(103, 25));
    btn_clear.setPreferredSize(new java.awt.Dimension(103, 25));
    btn_clear.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_clearActionPerformed(evt);
        }
    });

    btn_stampa.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/stampa.png"))); // NOI18N
    btn_stampa.setText("Stampa");
    btn_stampa.setMaximumSize(new java.awt.Dimension(103, 25));
    btn_stampa.setMinimumSize(new java.awt.Dimension(103, 25));
    btn_stampa.setPreferredSize(new java.awt.Dimension(103, 25));
    btn_stampa.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_stampaActionPerformed(evt);
        }
    });

    jLabel8.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
    jLabel8.setText("DATI CLIENTE");

    jLabel16.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
    jLabel16.setText("DATI ORDINE");

    cb_mpag.setMaximumRowCount(2);
    cb_mpag.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "PayPal", "Bonifico" }));
    cb_mpag.setSelectedIndex(-1);
    cb_mpag.setToolTipText("Seleziona una modalità di pagamento");
    cb_mpag.setEnabled(false);

    jLabel17.setText("# Tracking:");

    jLabel18.setText("Modalità Pagamento:");

    Tracking.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    Tracking.setEnabled(false);

    jLabel21.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
    jLabel21.setText("Note");

    btn_refresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    btn_refresh.setText("Aggiorna");
    btn_refresh.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_refreshActionPerformed(evt);
        }
    });

    note.setColumns(20);
    note.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    note.setLineWrap(true);
    note.setRows(5);
    jScrollPane1.setViewportView(note);

    info.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
    info.setForeground(new java.awt.Color(204, 0, 0));

    libriTrovati.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

    jLabel23.setText("Libri Trovati:");

    ritirato.setText("Ritirato?");

    spacchettato.setText("Spacchettato?");

    emailTemp.setText("Email");
    emailTemp.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            emailTempActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel1Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addComponent(jLabel11)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(search_for, javax.swing.GroupLayout.PREFERRED_SIZE, 423, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btn_salva, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_aggiorna, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGap(18, 18, 18)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(btn_clear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_cancella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(81, 81, 81)
                    .addComponent(emailTemp)
                    .addGap(18, 18, 18)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btn_stampa, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_refresh, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addComponent(jLabel10)
                .addComponent(jLabel21)
                .addComponent(jLabel8)
                .addComponent(jLabel16)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(6, 6, 6)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(Sped)
                        .addComponent(ritirato))
                    .addGap(18, 18, 18)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(PagSped)
                            .addGap(17, 17, 17)
                            .addComponent(jLabel18)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(cb_mpag, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(spacchettato)))
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel4)
                            .addGap(18, 18, 18)
                            .addComponent(Tel2, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGap(6, 6, 6)
                            .addComponent(jLabel17)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(Tracking, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel7)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGap(6, 6, 6)
                                    .addComponent(jLabel13))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGap(23, 23, 23)
                                    .addComponent(jLabel14)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(DataRitiro, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
                                .addComponent(Indirizzo, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(Citta)))
                        .addComponent(jLabel9)
                        .addComponent(jLabel2)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGap(73, 73, 73)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(Cognome, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
                                .addComponent(CodiceCliente))))
                    .addGap(18, 18, 18)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel3)
                                .addComponent(jLabel1)
                                .addComponent(jLabel5))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(Nome)
                                .addComponent(Tel1)
                                .addComponent(Email, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel23, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(CAP)
                                .addComponent(libriTrovati)
                                .addComponent(Provincia, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 521, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(20, 20, 20)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 1061, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addComponent(info)
                    .addGap(0, 0, Short.MAX_VALUE))))
    );
    jPanel1Layout.setVerticalGroup(
        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel1Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel8)
            .addGap(18, 18, 18)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel9)
                        .addComponent(CodiceCliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(Cognome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(Tel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(Nome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(Tel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(Email, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
            .addGap(31, 31, 31)
            .addComponent(jLabel16)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(DataRitiro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel23)
                .addComponent(libriTrovati, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel13)
                .addComponent(Indirizzo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel12)
                .addComponent(CAP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel14)
                .addComponent(Citta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(Provincia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel15))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel17)
                .addComponent(Tracking, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(Sped)
                .addComponent(PagSped)
                .addComponent(cb_mpag, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel18))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(ritirato)
                .addComponent(spacchettato))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(jLabel10)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel11)
                .addComponent(search_for, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addComponent(jLabel21)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(41, 41, 41)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btn_clear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btn_salva, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btn_stampa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(emailTemp))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btn_aggiorna, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btn_cancella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btn_refresh))
            .addContainerGap())
        .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(jScrollPane6)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(info)
            .addGap(41, 41, 41))
    );

    Pannello.addTab("Inserimento", jPanel1);

    ritirato1.setText("Ritirato?");

    spacchettato1.setText("Spacchettato?");

    btnModificaMultipla.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    btnModificaMultipla.setText("Aggiorna");
    btnModificaMultipla.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnModificaMultiplaActionPerformed(evt);
        }
    });

    jScrollPane7.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    jScrollPane7.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    tblModificaMultipla.setAutoCreateRowSorter(true);
    tblModificaMultipla.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    tblModificaMultipla.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {

        }
    )
    );
    tblModificaMultipla.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
    tblModificaMultipla.setRowHeight(25);
    jScrollPane7.setViewportView(tblModificaMultipla);

    btnModificaModificaMultipla.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/modifica.png"))); // NOI18N
    btnModificaModificaMultipla.setText("Modifica");
    btnModificaModificaMultipla.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnModificaModificaMultiplaActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
    jPanel9.setLayout(jPanel9Layout);
    jPanel9Layout.setHorizontalGroup(
        jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel9Layout.createSequentialGroup()
            .addGap(31, 31, 31)
            .addComponent(btnModificaMultipla)
            .addGap(46, 46, 46)
            .addComponent(ritirato1)
            .addGap(18, 18, 18)
            .addComponent(spacchettato1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(btnModificaModificaMultipla)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 1612, Short.MAX_VALUE)
    );
    jPanel9Layout.setVerticalGroup(
        jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel9Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ritirato1)
                    .addComponent(spacchettato1)
                    .addComponent(btnModificaModificaMultipla))
                .addComponent(btnModificaMultipla))
            .addGap(10, 10, 10)
            .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE))
    );

    Pannello.addTab("Modifica Multipla", jPanel9);

    jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    tbl_ritirioggi.setAutoCreateRowSorter(true);
    tbl_ritirioggi.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
    tbl_ritirioggi.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Codice Cliente", "Nome", "Cognome", "Email"
        }
    ) {
        boolean[] canEdit = new boolean [] {
            false, false, false, false
        };

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    tbl_ritirioggi.setDefaultEditor(Object.class, null);
    tbl_ritirioggi.setRowHeight(25);
    tbl_ritirioggi.getTableHeader().setReorderingAllowed(false);
    jScrollPane2.setViewportView(tbl_ritirioggi);

    btn_ritirioggi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    btn_ritirioggi.setText("Aggiorna");
    btn_ritirioggi.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_ritirioggiActionPerformed(evt);
        }
    });

    btn_stampaoggi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/stampa.png"))); // NOI18N
    btn_stampaoggi.setText("Stampa Ritiri di Oggi");
    btn_stampaoggi.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_stampaoggiActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel2Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(btn_ritirioggi)
            .addGap(18, 18, 18)
            .addComponent(btn_stampaoggi)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addGroup(jPanel2Layout.createSequentialGroup()
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 630, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(0, 982, Short.MAX_VALUE))
    );
    jPanel2Layout.setVerticalGroup(
        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btn_ritirioggi)
                .addComponent(btn_stampaoggi))
            .addGap(41, 41, 41)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE))
    );

    Pannello.addTab("Ritiri Oggi", jPanel2);

    btn_ritiridomani.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    btn_ritiridomani.setText("Aggiorna");
    btn_ritiridomani.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_ritiridomaniActionPerformed(evt);
        }
    });

    btn_stampadomani.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/stampa.png"))); // NOI18N
    btn_stampadomani.setText("Stampa Ritiri Domani");
    btn_stampadomani.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_stampadomaniActionPerformed(evt);
        }
    });

    jScrollPane3.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    tbl_ritiridomani.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
    tbl_ritiridomani.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Codice Cliente", "Nome", "Cognome", "Email"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }
    });
    tbl_ritiridomani.setCellSelectionEnabled(true);
    tbl_ritiridomani.setRowHeight(25);
    tbl_ritiridomani.setDefaultEditor(Object.class, null);
    jScrollPane3.setViewportView(tbl_ritiridomani);

    javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
    jPanel3.setLayout(jPanel3Layout);
    jPanel3Layout.setHorizontalGroup(
        jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel3Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(btn_ritiridomani)
            .addGap(18, 18, 18)
            .addComponent(btn_stampadomani)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addGroup(jPanel3Layout.createSequentialGroup()
            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 630, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(0, 982, Short.MAX_VALUE))
    );
    jPanel3Layout.setVerticalGroup(
        jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel3Layout.createSequentialGroup()
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btn_ritiridomani)
                .addComponent(btn_stampadomani))
            .addGap(41, 41, 41)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE))
    );

    Pannello.addTab("Ritiri Domani", jPanel3);

    jScrollPane4.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    tbl_ritirigiorno.setAutoCreateRowSorter(true);
    tbl_ritirigiorno.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    tbl_ritirigiorno.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Codice Cliente", "Nome", "Cognome", "Telefono1", "Telefono2", "Email"
        }
    ));
    tbl_ritirigiorno.setCellSelectionEnabled(true);
    tbl_ritirigiorno.setRowHeight(25);
    tbl_ritirigiorno.setDefaultEditor(Object.class, null);
    jScrollPane4.setViewportView(tbl_ritirigiorno);

    jLabel6.setText("Ritiri per il giorno:");

    giornoRitiro.addActionListener(new ActionListener(){

        public void actionPerformed(ActionEvent e){
            try{
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                String sql = "select CodiceCliente, Nome, Cognome, Telefono1, Telefono2, Email from ordini where dRitiro='"+giornoRitiro.getText()+"' ORDER BY CodiceCliente ASC";
                Statement = conn.prepareStatement(sql);
                rs = Statement.executeQuery();
                tbl_ritirigiorno.setModel(DbUtils.resultSetToTableModel(rs));
                Statement.close();
                conn.close();
            }catch (Exception ex){
                JOptionPane.showMessageDialog(null, ex);
            }
        }
    });

    btn_ritiriPerGiorno.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    btn_ritiriPerGiorno.setText("Aggiorna");
    btn_ritiriPerGiorno.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_ritiriPerGiornoActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
    jPanel4.setLayout(jPanel4Layout);
    jPanel4Layout.setHorizontalGroup(
        jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel4Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel6)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(giornoRitiro, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(85, 85, 85)
            .addComponent(btn_ritiriPerGiorno)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1612, Short.MAX_VALUE)
    );
    jPanel4Layout.setVerticalGroup(
        jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel4Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addComponent(btn_ritiriPerGiorno)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(3, 3, 3)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(giornoRitiro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 646, Short.MAX_VALUE))))
    );

    Pannello.addTab("Ritiri per Giorno", jPanel4);

    btn_spedizioni.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    btn_spedizioni.setText("Aggiorna");
    btn_spedizioni.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_spedizioniActionPerformed(evt);
        }
    });

    tbl_spedizioni.setAutoCreateRowSorter(true);
    tbl_spedizioni.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    tbl_spedizioni.setRowHeight(25);
    tbl_spedizioni.setDefaultEditor(Object.class, null);
    jScrollPane5.setViewportView(tbl_spedizioni);

    javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
    jPanel5.setLayout(jPanel5Layout);
    jPanel5Layout.setHorizontalGroup(
        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel5Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(btn_spedizioni)
            .addGap(0, 1505, Short.MAX_VALUE))
        .addGroup(jPanel5Layout.createSequentialGroup()
            .addGap(19, 19, 19)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 1583, Short.MAX_VALUE)
            .addContainerGap())
    );
    jPanel5Layout.setVerticalGroup(
        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(btn_spedizioni)
            .addGap(18, 18, 18)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
            .addGap(55, 55, 55))
    );

    Pannello.addTab("Spedizioni", jPanel5);

    btn_evasi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    btn_evasi.setText("Aggiorna");
    btn_evasi.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_evasiActionPerformed(evt);
        }
    });

    tbl_evasi.setAutoCreateRowSorter(true);
    tbl_evasi.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    tbl_evasi.setRowHeight(25);
    tbl_evasi.setDefaultEditor(Object.class, null);

    tbl_evasi.addMouseListener(new MouseAdapter(){
        @Override
        public void mouseClicked(MouseEvent e){
            if(e.getClickCount()==2){
                modificaDati(tbl_evasi);
            }
        }
    });

    tbl_evasi.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
    tbl_evasi.getActionMap().put("Enter", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            modificaDati(tbl_evasi);
        }
    });
    jScrollPane8.setViewportView(tbl_evasi);

    jLabel24.setText("Cerca:");

    cercaEvasi.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    cercaEvasi.addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyReleased(java.awt.event.KeyEvent evt) {
            cercaEvasiKeyReleased(evt);
        }
    });

    javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
    jPanel7.setLayout(jPanel7Layout);
    jPanel7Layout.setHorizontalGroup(
        jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel7Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(btn_evasi)
            .addGap(146, 146, 146)
            .addComponent(jLabel24)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(cercaEvasi, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(1053, Short.MAX_VALUE))
        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 1583, Short.MAX_VALUE)
                .addGap(15, 15, 15)))
    );
    jPanel7Layout.setVerticalGroup(
        jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel7Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btn_evasi)
                .addComponent(jLabel24)
                .addComponent(cercaEvasi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(645, Short.MAX_VALUE))
        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(54, 54, 54)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
                .addGap(55, 55, 55)))
    );

    Pannello.addTab("Evasi", jPanel7);

    btn_spedizioniEvase.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    btn_spedizioniEvase.setText("Aggiorna");
    btn_spedizioniEvase.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            btn_spedizioniEvaseActionPerformed(evt);
        }
    });

    tbl_spedizioniEvase.setAutoCreateRowSorter(true);
    tbl_spedizioniEvase.setDefaultEditor(Object.class, null);
    tbl_spedizioniEvase.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    tbl_spedizioniEvase.setRowHeight(25);
    tbl_evasi.setDefaultEditor(Object.class, null);

    tbl_spedizioniEvase.addMouseListener(new MouseAdapter(){
        @Override
        public void mouseClicked(MouseEvent e){
            if(e.getClickCount()==2){
                modificaDati(tbl_spedizioniEvase);
            }
        }
    });

    tbl_spedizioniEvase.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
    tbl_spedizioniEvase.getActionMap().put("Enter", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            modificaDati(tbl_spedizioniEvase);
        }
    });
    jScrollPane9.setViewportView(tbl_spedizioniEvase);

    javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
    jPanel8.setLayout(jPanel8Layout);
    jPanel8Layout.setHorizontalGroup(
        jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel8Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(btn_spedizioniEvase)
            .addContainerGap(1505, Short.MAX_VALUE))
        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 1583, Short.MAX_VALUE)
                .addGap(15, 15, 15)))
    );
    jPanel8Layout.setVerticalGroup(
        jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel8Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(btn_spedizioniEvase)
            .addContainerGap(650, Short.MAX_VALUE))
        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(54, 54, 54)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
                .addGap(55, 55, 55)))
    );

    Pannello.addTab("Spedizione Evase", jPanel8);

    jMenuBar1.setToolTipText("");
    jMenuBar1.setMinimumSize(new java.awt.Dimension(0, 4));
    jMenuBar1.setPreferredSize(new java.awt.Dimension(27, 23));

    file_menu.setMnemonic('f');
    file_menu.setText("File");

    mn_salva.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
    mn_salva.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/salva.png"))); // NOI18N
    mn_salva.setText("Salva");
    mn_salva.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_salvaActionPerformed(evt);
        }
    });
    file_menu.add(mn_salva);

    mn_aggiorna.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
    mn_aggiorna.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/modifica.png"))); // NOI18N
    mn_aggiorna.setText("Modifica");
    mn_aggiorna.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_aggiornaActionPerformed(evt);
        }
    });
    file_menu.add(mn_aggiorna);

    mn_cancella.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
    mn_cancella.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/cancella.png"))); // NOI18N
    mn_cancella.setText("Cancella");
    mn_cancella.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_cancellaActionPerformed(evt);
        }
    });
    file_menu.add(mn_cancella);

    mn_elimina.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
    mn_elimina.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/elimina.png"))); // NOI18N
    mn_elimina.setText("Elimina");
    mn_elimina.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_eliminaActionPerformed(evt);
        }
    });
    file_menu.add(mn_elimina);

    mn_aggiornaTabella.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
    mn_aggiornaTabella.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/aggiorna.png"))); // NOI18N
    mn_aggiornaTabella.setText("Aggiorna");
    mn_aggiornaTabella.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_aggiornaTabellaActionPerformed(evt);
        }
    });
    file_menu.add(mn_aggiornaTabella);

    jMenu2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/stampa.png"))); // NOI18N
    jMenu2.setText("Stampa..");

    mn_stampa.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, java.awt.event.InputEvent.SHIFT_MASK));
    mn_stampa.setText("Stampa da Inserimento");
    mn_stampa.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_stampaActionPerformed(evt);
        }
    });
    jMenu2.add(mn_stampa);

    mn_stampaoggi.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, java.awt.event.InputEvent.SHIFT_MASK));
    mn_stampaoggi.setText("Stampa Ritiri Oggi");
    mn_stampaoggi.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_stampaoggiActionPerformed(evt);
        }
    });
    jMenu2.add(mn_stampaoggi);

    mn_stampadomani.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, java.awt.event.InputEvent.SHIFT_MASK));
    mn_stampadomani.setText("Stampa Ritiri Domani");
    mn_stampadomani.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_stampadomaniActionPerformed(evt);
        }
    });
    jMenu2.add(mn_stampadomani);

    file_menu.add(jMenu2);
    file_menu.add(jSeparator1);

    mn_impostazioni.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK));
    mn_impostazioni.setIcon(new javax.swing.ImageIcon(getClass().getResource("/immagini/impostazioni.png"))); // NOI18N
    mn_impostazioni.setText("Impostazioni");
    mn_impostazioni.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_impostazioniActionPerformed(evt);
        }
    });
    file_menu.add(mn_impostazioni);
    file_menu.add(jSeparator2);

    mn_esci.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
    mn_esci.setText("Esci");
    mn_esci.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            mn_esciActionPerformed(evt);
        }
    });
    file_menu.add(mn_esci);

    jMenuBar1.add(file_menu);

    setJMenuBar(jMenuBar1);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(Pannello, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(Pannello, javax.swing.GroupLayout.DEFAULT_SIZE, 714, Short.MAX_VALUE)
    );

    pack();
    }// </editor-fold>//GEN-END:initComponents

    private void get_clientiMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_get_clientiMouseClicked
        try{
            int row = get_clienti.convertRowIndexToModel(get_clienti.getSelectedRow());
            String Table_Click = (get_clienti.getModel().getValueAt(row, 0).toString());
            String sql = "select * from ordini where CodiceCliente='"+Table_Click+"'";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            ResultSet rs = Statement.executeQuery();
            if(rs.next()){
                String add1 = rs.getString("CodiceCliente");
                CodiceCliente.setText(add1);
                CC = add1;

                String add2 = rs.getString("Nome");
                Nome.setText(add2);

                String add3 = rs.getString("Cognome");
                Cognome.setText(add3);

                String add4 = rs.getString("Telefono1");
                Tel1.setText(add4);

                String add5 = rs.getString("Telefono2");
                Tel2.setText(add5);

                String add6 = rs.getString("Email");
                Email.setText(add6);

                String add7 = rs.getString("dRitiro");
                DataRitiro.setText(add7);
                
                String add12 = rs.getString("Indirizzo");
                Indirizzo.setText(add12);

                String add13 = rs.getString("CAP");
                CAP.setText(add13);

                String add14 = rs.getString("Citta");
                Citta.setText(add14);

                String add15 = rs.getString("Provincia");
                Provincia.setText(add15);
                
                String add16 = rs.getString("LibriTrovati");
                libriTrovati.setText(add16);

                String add8 = rs.getString("Spedizione");
                String add9 = rs.getString("SpedPagata");
                String add10 = rs.getString("MPagamento");
                String add11 = rs.getString("Tracking");
                Tracking.setText(add11);
                String add20 = rs.getString("Note");
                note.setText(add20);
                if(!(add8 == null)){
                    //Imposta Check box Spedizione
                    if(add8.compareTo("SI") == 0){
                        Sped.setSelected(true);

                        //Imposta ComboBox
                        if(add10 == null || add10.compareTo("") == 0){
                            cb_mpag.setSelectedIndex(-1);
                        }else
                            if(add10.compareTo("PayPal") == 0){
                                cb_mpag.setSelectedIndex(0);
                            } else
                                cb_mpag.setSelectedIndex(1);


                        //Imposta Check Box Spedizione Pagata
                        if(add9.compareTo("SI") == 0){
                            PagSped.setSelected(true);
                        } else
                            PagSped.setSelected(false);
                    } else{
                        Sped.setSelected(false);
                        PagSped.setSelected(false);
                        cb_mpag.setSelectedIndex(-1);
                    }
                } else{
                    Sped.setSelected(false);
                    PagSped.setSelected(false);
                    cb_mpag.setSelectedIndex(-1);
                }

            }
            Statement.close();
            conn.close();
        }catch(Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e, "Errore a1", JOptionPane.ERROR_MESSAGE);
            logger.error("Errore in get_clientiMouseClicked", e);
        }
    }//GEN-LAST:event_get_clientiMouseClicked

    private void get_clientiKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_get_clientiKeyReleased
        if(evt.getKeyCode() == KeyEvent.VK_DOWN || evt.getKeyCode() == KeyEvent.VK_UP){
            try{

                int row = get_clienti.convertRowIndexToModel(get_clienti.getSelectedRow());
                String Table_Click = (get_clienti.getModel().getValueAt(row, 0).toString());
                String sql = "select * from ordini where CodiceCliente='"+Table_Click+"'";
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement = conn.prepareStatement(sql);
                ResultSet rs = Statement.executeQuery();
                if(rs.next()){
                    String add1 = rs.getString("CodiceCliente");
                    CodiceCliente.setText(add1);
                    CC = add1;

                    String add2 = rs.getString("Nome");
                    Nome.setText(add2);

                    String add3 = rs.getString("Cognome");
                    Cognome.setText(add3);

                    String add4 = rs.getString("Telefono1");
                    Tel1.setText(add4);

                    String add5 = rs.getString("Telefono2");
                    Tel2.setText(add5);

                    String add6 = rs.getString("Email");
                    Email.setText(add6);

                    String add7 = rs.getString("dRitiro");
                    DataRitiro.setText(add7);
                    
                    String add12 = rs.getString("Indirizzo");
                    Indirizzo.setText(add12);
                    
                    String add13 = rs.getString("CAP");
                    CAP.setText(add13);
                    
                    String add14 = rs.getString("Citta");
                    Citta.setText(add14);
                    
                    String add15 = rs.getString("Provincia");
                    Provincia.setText(add15);

                    String add8 = rs.getString("Spedizione");
                    String add9 = rs.getString("SpedPagata");
                    String add10 = rs.getString("MPagamento");
                    String add11 = rs.getString("Tracking");
                    Tracking.setText(add11);
                    String add20 = rs.getString("Note");
                    note.setText(add20);
                    
                    String add17 = rs.getString("LibriTrovati");
                    libriTrovati.setText(add17);

                    if(!(add8 == null)){
                        //Imposta Check box Spedizione
                        if(add8.compareTo("SI") == 0){
                            Sped.setSelected(true);
                            
                            //Imposta ComboBox
                            if(add10 == null || add10.compareTo("") == 0){
                            cb_mpag.setSelectedIndex(-1);
                        }else
                            if(add10.compareTo("PayPal") == 0){
                                cb_mpag.setSelectedIndex(0);
                            } else
                                cb_mpag.setSelectedIndex(1);
                            
                            
                            //Imposta Check Box Spedizione Pagata
                            if(add9.compareTo("SI") == 0){
                                PagSped.setSelected(true);
                            } else
                                PagSped.setSelected(false);
                        } else{
                            Sped.setSelected(false);
                            PagSped.setSelected(false);
                            cb_mpag.setSelectedIndex(-1);
                        }
                    } else{
                        Sped.setSelected(false);
                        PagSped.setSelected(false);
                        cb_mpag.setSelectedIndex(-1);
                        
                    }

                }
                enablerFromAction();
                Statement.close();
                conn.close();
            }catch(SQLException e){
                JOptionPane.showMessageDialog(null, e);
                e.printStackTrace();
                logger.error("Errore in get_clientiKeyReleased", e);
            }
        }
    }//GEN-LAST:event_get_clientiKeyReleased

    private void search_forKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_search_forKeyReleased
        String query = search_for.getText().toLowerCase();

        filter(query,get_clienti);

    }//GEN-LAST:event_search_forKeyReleased

    private void btn_salvaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_salvaActionPerformed
        salva();
    }//GEN-LAST:event_btn_salvaActionPerformed

    private void btn_aggiornaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_aggiornaActionPerformed
        aggiorna();
    }//GEN-LAST:event_btn_aggiornaActionPerformed

    private void btn_cancellaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_cancellaActionPerformed
        elimina();
    }//GEN-LAST:event_btn_cancellaActionPerformed

    private void btn_clearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_clearActionPerformed
        try{
            clear_fields();
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in btn_clearActionPerformed", e);
        }
    }//GEN-LAST:event_btn_clearActionPerformed

    private void btn_stampaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_stampaActionPerformed

        stampaInserimento();
    }//GEN-LAST:event_btn_stampaActionPerformed

    private void btn_ritirioggiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ritirioggiActionPerformed

        try{
            DateFormat dateFormat = new SimpleDateFormat("dd/MM");
            Date oggi = new Date();
            String sql = "select CodiceCliente, Nome, Cognome, Email, LibriTrovati from ordini where dRitiro='"+dateFormat.format(oggi)+"' AND Ritirato = 'NO' AND Spacchettato = 'NO' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            tbl_ritirioggi.setModel(DbUtils.resultSetToTableModel(rs));
            Statement.close();
            conn.close();
        }catch (SQLException e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in btn_ritirioggiActionPerformed", e);

        }
    }//GEN-LAST:event_btn_ritirioggiActionPerformed

    private void btn_stampaoggiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_stampaoggiActionPerformed
        stampaOggi();
    }//GEN-LAST:event_btn_stampaoggiActionPerformed

    private void btn_ritiridomaniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ritiridomaniActionPerformed

        try{
            DateFormat dateFormat = new SimpleDateFormat("dd/MM");
            Date domani = new Date();
            domani.setDate(domani.getDate() + 1);
            String sql = "select CodiceCliente, Nome, Cognome, Email, LibriTrovati from ordini where dRitiro='"+dateFormat.format(domani)+"' AND Ritirato = 'NO' AND Spacchettato = 'NO' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            tbl_ritiridomani.setModel(DbUtils.resultSetToTableModel(rs));
            Statement.close();
            conn.close();
        }catch (SQLException e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in btn_ritiridomaniActionPerformed", e);

        }
    }//GEN-LAST:event_btn_ritiridomaniActionPerformed

    private void btn_stampadomaniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_stampadomaniActionPerformed
        stampaDomani();
    }//GEN-LAST:event_btn_stampadomaniActionPerformed

    private void btn_ritiriPerGiornoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ritiriPerGiornoActionPerformed
        try{
            String sql = "select CodiceCliente, Nome, Cognome, Telefono1, Telefono2, Email from ordini where dRitiro='"+giornoRitiro.getText()+"' ORDER BY CodiceCliente ASC";
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement = conn.prepareStatement(sql);
            rs = Statement.executeQuery();
            tbl_ritirigiorno.setModel(DbUtils.resultSetToTableModel(rs));
            Statement.close();
            conn.close();
        }catch (SQLException e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in btn_ritiriPerGiornoActionPerformed", e);
        }
    }//GEN-LAST:event_btn_ritiriPerGiornoActionPerformed

    private void btn_spedizioniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_spedizioniActionPerformed
        populate_table_spedizioni();
    }//GEN-LAST:event_btn_spedizioniActionPerformed

    private void mn_esciActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_esciActionPerformed
        int conf = JOptionPane.showConfirmDialog(null, "Chiudere?", "Exit",JOptionPane.YES_NO_OPTION);
        if (conf == 0){
            System.exit(0);
        }
    }//GEN-LAST:event_mn_esciActionPerformed

    private void mn_impostazioniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_impostazioniActionPerformed
        
        Impostazioni.setVisible(true);
        Impostazioni.setMinimumSize(new Dimension(727, 460));
        dbCorrente.setText(dbPath);
    }//GEN-LAST:event_mn_impostazioniActionPerformed

    private void btn_closeImpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_closeImpActionPerformed
        Impostazioni.setVisible(false);
    }//GEN-LAST:event_btn_closeImpActionPerformed

    private void btn_impDbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_impDbActionPerformed
        
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("db File", "db");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File("."));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.showOpenDialog(null);
        File selectedPfile = chooser.getSelectedFile();
        if (!(selectedPfile == null)){
            String db = selectedPfile.getAbsolutePath().replace("\\" , "/");
            pathDb.setText(db);
        }
    }//GEN-LAST:event_btn_impDbActionPerformed

    private void btn_salvaDbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_salvaDbActionPerformed
       if(!(pathDb.getText().equals(""))){
            dbPath = pathDb.getText();
            update_table();
           //scrittura sul file del database
           File temp = null;
           try {
               temp = new File(OrdiniGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
           } catch (URISyntaxException ex) {
               logger.error("Errore in btn_salvaDbActionPerformed", ex);
           }
            String jarDir = temp.getParentFile().getPath();
            createFilePath(jarDir);
            dbCorrente.setText(dbPath);
            pathDb.setText("");
       }else{
            JOptionPane.showMessageDialog(null, "Nessun database selezionato", "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btn_salvaDbActionPerformed

    private void mn_aggiornaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_aggiornaActionPerformed
        aggiorna();
    }//GEN-LAST:event_mn_aggiornaActionPerformed

    private void mn_cancellaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_cancellaActionPerformed
        try{
            clear_fields();
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
            logger.error("Errore in mn_cancellaActionPerformed", e);
        }
    }//GEN-LAST:event_mn_cancellaActionPerformed

    private void mn_eliminaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_eliminaActionPerformed
        elimina();
    }//GEN-LAST:event_mn_eliminaActionPerformed

    private void mn_stampaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_stampaActionPerformed
        stampaInserimento();
    }//GEN-LAST:event_mn_stampaActionPerformed

    private void mn_stampaoggiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_stampaoggiActionPerformed
        stampaOggi();
    }//GEN-LAST:event_mn_stampaoggiActionPerformed

    private void mn_stampadomaniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_stampadomaniActionPerformed
        stampaDomani();
    }//GEN-LAST:event_mn_stampadomaniActionPerformed

    private void mn_salvaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_salvaActionPerformed
        salva();
    }//GEN-LAST:event_mn_salvaActionPerformed

    private void btn_nuovoDbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_nuovoDbActionPerformed
        String[] options = new String[2];
        options[0]="      Si      ";
        options[1]="      No      ";
        int opt = JOptionPane.showOptionDialog(null,"Cancellare il database corrente e crearne uno nuovo? ","Database", 0,JOptionPane.PLAIN_MESSAGE,null,options,null);
            if(opt == 0){
                cancellaDb();
                update_table();
                populate_table_spedizioni();
                populate_table_ritiri_oggi();
                populate_table_ritiri_domani();
            }
    }//GEN-LAST:event_btn_nuovoDbActionPerformed

    private void btn_refreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_refreshActionPerformed
        update_table();
        populate_table_spedizioni();
        populate_table_ritiri_oggi();
        populate_table_ritiri_domani();
        populate_table_evasi();
    }//GEN-LAST:event_btn_refreshActionPerformed

    private void mn_aggiornaTabellaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_aggiornaTabellaActionPerformed
        update_table();
        populate_table_spedizioni();
        populate_table_ritiri_oggi();
        populate_table_ritiri_domani();
        populate_table_evasi();
    }//GEN-LAST:event_mn_aggiornaTabellaActionPerformed

    private void btn_evasiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_evasiActionPerformed
        populate_table_evasi();
    }//GEN-LAST:event_btn_evasiActionPerformed

    private void cercaEvasiKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_cercaEvasiKeyReleased
        String query = cercaEvasi.getText().toLowerCase();
        filter(query,tbl_evasi);
    }//GEN-LAST:event_cercaEvasiKeyReleased

    private void btn_spedizioniEvaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_spedizioniEvaseActionPerformed
        populate_table_spedizioniEvase();
    }//GEN-LAST:event_btn_spedizioniEvaseActionPerformed

    private void btnModificaMultiplaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnModificaMultiplaActionPerformed
        populate_table_ModificaMultipla();
    }//GEN-LAST:event_btnModificaMultiplaActionPerformed

    private void btnModificaModificaMultiplaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnModificaModificaMultiplaActionPerformed
        try{
            String[] codiciModificati = null;
            

            //Seleziona tutte le row
            int[] OldRows = tblModificaMultipla.getSelectedRows();
            int[] rows = new int[OldRows.length];
            for(int i = OldRows.length - 1; i>=0; i--){
                rows[i] = tblModificaMultipla.convertRowIndexToModel(OldRows[i]);
            }
            
            String Ritirato = null;
            String Spacchettato = null;
            DefaultTableModel tm = (DefaultTableModel)tblModificaMultipla.getModel(); 
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            //Applica spacchettato oppure ritirato per ognuna
            for(int i=rows.length-1; i>=0; i--){

                if(ritirato1.isSelected()){
                    Ritirato = "SI";
                }else 
                    Ritirato = "NO";
                if(spacchettato1.isSelected()){
                    Spacchettato = "SI";
                }else 
                    Spacchettato = "NO";
                
                String sql = "update ordini set Spacchettato='"+Spacchettato+"',Ritirato='"+Ritirato+"' where CodiceCliente ='"+tm.getValueAt(rows[i], 0).toString()+"'";
                Statement = conn.prepareStatement(sql);
                Statement.execute();
                Statement.close();
            }
            conn.close();
            aggiornaTabelle();
            
            ritirato1.setSelected(false);
            spacchettato1.setSelected(false);
            
            JOptionPane.showMessageDialog(null, "Clienti Modificati", "Modificati", JOptionPane.INFORMATION_MESSAGE);
        }catch(SQLException e){
            e.printStackTrace();
            logger.error("Errore in btnModificaModificaMultiplaActionPerformed", e);
        }
        
    }//GEN-LAST:event_btnModificaModificaMultiplaActionPerformed

    private void invioEmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invioEmailActionPerformed
        try {
            MimeMessage email = TestoEmail.createEmail(destinatario.getText(), "me", oggetto.getText(), testo.getText());
            Message messaggio = TestoEmail.sendMessage(service, "me", email);
            
            DateFormat dateFormat = new SimpleDateFormat("HH:mm");
            Calendar cal = Calendar.getInstance();
            info.setText(dateFormat.format(cal.getTime())+": Email Inviata!");
        } catch (MessagingException | IOException ex) {
            ex.printStackTrace();
            logger.error("Errore in invioEmailActionPerformed", ex);
        }
    }//GEN-LAST:event_invioEmailActionPerformed

    private void emailTempActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emailTempActionPerformed
        InvioEmail.setVisible(true);
        InvioEmail.setMinimumSize(new Dimension(916, 611));
    }//GEN-LAST:event_emailTempActionPerformed

    private void visualizzaEmailImpostazioniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_visualizzaEmailImpostazioniActionPerformed
        int selezione = cbEmailImpostazioni.getSelectedIndex();
        String sql;
        
        //Usiamo uno switch per scegliere quali delle tre opzioni usare
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            switch (selezione){
                case 0: sql = "select EmailConTelefono from email";
                        Statement = conn.prepareStatement(sql);
                        ResultSet rs1 = Statement.executeQuery();
                        epEmailImpostazioni.setText(rs1.getString("EmailConTelefono"));
                        rs1.close();
                        break;
                case 1: sql = "select EmailSenzaTelefono from email";
                        Statement = conn.prepareStatement(sql);
                        ResultSet rs2 = Statement.executeQuery();
                        epEmailImpostazioni.setText(rs2.getString("EmailSenzaTelefono"));
                        rs2.close();
                        break;
                case 2: sql = "select EmailNoLibri from email";
                        Statement = conn.prepareStatement(sql);
                        ResultSet rs3 = Statement.executeQuery();
                        epEmailImpostazioni.setText(rs3.getString("EmailNoLibri"));
                        rs3.close();
                        break;
            }
            Statement.close();
            
            conn.close();
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            logger.error("Errore di visualizzaEmail", ex);
        }
    }//GEN-LAST:event_visualizzaEmailImpostazioniActionPerformed

    private void salvaEmailImpostazioniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salvaEmailImpostazioniActionPerformed
        int selezione = cbEmailImpostazioni.getSelectedIndex();
        String sql;
        String testoEmail = epEmailImpostazioni.getText().replace("'","''");
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            switch (selezione){
                case 0: sql = "update email set EmailConTelefono='"+testoEmail+"'";
                        Statement = conn.prepareStatement(sql);
                        Statement.execute();
                        break;
                case 1: sql = "update email set EmailSenzaTelefono='"+testoEmail+"'";
                        Statement = conn.prepareStatement(sql);
                        Statement.execute();
                        break;
                case 2: sql = "update email set EmailNoLibri='"+testoEmail+"'";
                        Statement = conn.prepareStatement(sql);
                        Statement.execute();
                        break;
            }
            
            Statement.close();
            conn.close();
            
            JOptionPane.showMessageDialog(null, "Testo Email modificato!", "Email Modificata", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            logger.error("Errore di SalvaEmail", ex);
        }
    }//GEN-LAST:event_salvaEmailImpostazioniActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OrdiniGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OrdiniGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OrdiniGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OrdiniGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OrdiniGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField CAP;
    private javax.swing.JTextField Citta;
    private javax.swing.JTextField CodiceCliente;
    private javax.swing.JTextField Cognome;
    private javax.swing.JTextField DataRitiro;
    private javax.swing.JTextField Email;
    private javax.swing.JFrame Impostazioni;
    private javax.swing.JTextField Indirizzo;
    private javax.swing.JFrame InvioEmail;
    private javax.swing.JTextField Nome;
    private javax.swing.JCheckBox PagSped;
    private javax.swing.JTabbedPane Pannello;
    private javax.swing.JTextField Provincia;
    private javax.swing.JCheckBox Sped;
    private javax.swing.JTextField Tel1;
    private javax.swing.JTextField Tel2;
    private javax.swing.JTextField Tracking;
    private javax.swing.JButton btnModificaModificaMultipla;
    private javax.swing.JButton btnModificaMultipla;
    private javax.swing.JButton btn_aggiorna;
    private javax.swing.JButton btn_cancella;
    private javax.swing.JButton btn_clear;
    private javax.swing.JButton btn_closeImp;
    private javax.swing.JButton btn_evasi;
    private javax.swing.JButton btn_impDb;
    private javax.swing.JButton btn_nuovoDb;
    private javax.swing.JButton btn_refresh;
    private javax.swing.JButton btn_ritiriPerGiorno;
    private javax.swing.JButton btn_ritiridomani;
    private javax.swing.JButton btn_ritirioggi;
    private javax.swing.JButton btn_salva;
    private javax.swing.JButton btn_salvaDb;
    private javax.swing.JButton btn_spedizioni;
    private javax.swing.JButton btn_spedizioniEvase;
    private javax.swing.JButton btn_stampa;
    private javax.swing.JButton btn_stampadomani;
    private javax.swing.JButton btn_stampaoggi;
    private javax.swing.JComboBox<String> cbEmailImpostazioni;
    private javax.swing.JComboBox<String> cb_mpag;
    private javax.swing.JTextField cercaEvasi;
    private javax.swing.JLabel dbCorrente;
    private javax.swing.JTextField destinatario;
    private javax.swing.JButton emailTemp;
    private javax.swing.JTextPane epEmailImpostazioni;
    private javax.swing.JMenu file_menu;
    private javax.swing.JTable get_clienti;
    private javax.swing.JTextField giornoRitiro;
    private javax.swing.JLabel info;
    private javax.swing.JButton invioEmail;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblEmailImpostazioni;
    private javax.swing.JTextField libriTrovati;
    private javax.swing.JMenuItem mn_aggiorna;
    private javax.swing.JMenuItem mn_aggiornaTabella;
    private javax.swing.JMenuItem mn_cancella;
    private javax.swing.JMenuItem mn_elimina;
    private javax.swing.JMenuItem mn_esci;
    private javax.swing.JMenuItem mn_impostazioni;
    private javax.swing.JMenuItem mn_salva;
    private javax.swing.JMenuItem mn_stampa;
    private javax.swing.JMenuItem mn_stampadomani;
    private javax.swing.JMenuItem mn_stampaoggi;
    private javax.swing.JTextArea note;
    private javax.swing.JTextField oggetto;
    private javax.swing.JTextField pathDb;
    private javax.swing.JCheckBox ritirato;
    private javax.swing.JCheckBox ritirato1;
    private javax.swing.JButton salvaEmailImpostazioni;
    private javax.swing.JTextField search_for;
    private javax.swing.JCheckBox spacchettato;
    private javax.swing.JCheckBox spacchettato1;
    private javax.swing.JTable tblModificaMultipla;
    private javax.swing.JTable tbl_evasi;
    private javax.swing.JTable tbl_ritiridomani;
    private javax.swing.JTable tbl_ritirigiorno;
    private javax.swing.JTable tbl_ritirioggi;
    private javax.swing.JTable tbl_spedizioni;
    private javax.swing.JTable tbl_spedizioniEvase;
    private javax.swing.JEditorPane testo;
    private javax.swing.JButton visualizzaEmailImpostazioni;
    // End of variables declaration//GEN-END:variables
}
