/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.gui.components;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import rappsilber.utils.xibioedacuk_cert;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class FeedBack extends javax.swing.JPanel {

    private String UserIDProperty = "xiSEARCH_UserID";
    
    /**
     * Creates new form FeedBack
     */
    public FeedBack() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        spResponse = new javax.swing.JScrollPane();
        txtResponse = new javax.swing.JTextArea();
        btnSend = new javax.swing.JButton();
        slRating = new javax.swing.JSlider();
        ckRateXi = new javax.swing.JCheckBox();
        lblBad = new javax.swing.JLabel();
        lblGood = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();

        setBorder(javax.swing.BorderFactory.createEtchedBorder());

        txtResponse.setColumns(20);
        txtResponse.setRows(5);
        spResponse.setViewportView(txtResponse);

        btnSend.setText("Send");
        btnSend.setFocusCycleRoot(true);
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        slRating.setMaximum(5);
        slRating.setMinimum(-5);
        slRating.setValue(0);
        slRating.setEnabled(false);

        ckRateXi.setText("Rate xiSEARCH");
        ckRateXi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckRateXiActionPerformed(evt);
            }
        });

        lblBad.setText("Bad");
        lblBad.setEnabled(false);

        lblGood.setText("Good");
        lblGood.setEnabled(false);

        jLabel3.setText("Comments/Feature Requests:");

        jLabel4.setText("E-Mail (Optional)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spResponse, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtEmail)
                .addGap(18, 18, 18)
                .addComponent(btnSend))
            .addGroup(layout.createSequentialGroup()
                .addComponent(ckRateXi)
                .addGap(18, 18, 18)
                .addComponent(lblBad)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(slRating, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblGood))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(lblBad)
                    .addComponent(ckRateXi)
                    .addComponent(slRating, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblGood))
                .addGap(7, 7, 7)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spResponse, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSend)
                    .addComponent(jLabel4)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void ckRateXiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckRateXiActionPerformed
        lblBad.setEnabled(ckRateXi.isSelected());
        lblGood.setEnabled(ckRateXi.isSelected());
        slRating.setEnabled(ckRateXi.isSelected());
        if (ckRateXi.isSelected())
            btnSend.setEnabled(true);
    }//GEN-LAST:event_ckRateXiActionPerformed

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        btnSend.setEnabled(false);
        Runnable runnable = new Runnable() {
            public void run() {
                try {                    
                    String urlParameters = (ckRateXi.isSelected() ? "rate=" + slRating.getValue() : "")
                            + (txtEmail.getText().isEmpty() ? "" : "&email=" + txtEmail.getText())
                            + (txtResponse.getText().isEmpty() ? "" : "&response=" + txtResponse.getText());
                    byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                    int postDataLength = postData.length;
                    String request = "https://rappsilberlab.org/xiversion/feedback.php";
                    URL url = new URL(request);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("charset", "utf-8");
                    conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                    conn.setUseCaches(false);
                    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                        wr.write(postData);
                    }
                    Logger.getLogger(FeedBack.class.getName()).log(Level.INFO, "Sending feedback");
                    
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder resp = new StringBuilder();
                        while ((inputLine = in.readLine()) != null)
                            resp.append(inputLine).append("\n");
                        in.close();
                        Logger.getLogger(CallBackSettings.class.getName()).log(Level.INFO, "Response:"  + resp.toString());
                    } catch (Exception e) {}
                    
                    int resp = conn.getResponseCode();
                    Logger.getLogger(FeedBack.class.getName()).log(Level.INFO, "Sending feedback Response:" + conn.getResponseCode());
                    if (resp != 200) {
                        Logger.getLogger(FeedBack.class.getName()).log(Level.INFO, "Sending feedback Failed:" + resp);
                        JOptionPane.showMessageDialog(FeedBack.this, "Could Not Send the Feedback\nResponse:" + resp, "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        Logger.getLogger(FeedBack.class.getName()).log(Level.INFO, "Feedback was send");
                        JOptionPane.showMessageDialog(FeedBack.this, "Feedback was send");
                    }
                    
                } catch (IOException ex) {
                    Logger.getLogger(FeedBack.class.getName()).log(Level.SEVERE, "Error sending feedback", ex);
                    JOptionPane.showMessageDialog(FeedBack.this, "Could Not Send the Feedback", "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    SwingUtilities.invokeLater( new Runnable() {
                        @Override
                        public void run() {
                            btnSend.setEnabled(true);
                        }
                    });
                }
                
            }
        };
        Thread send = new Thread(runnable);
        send.setName("Send Response");
        send.start();
                
    }//GEN-LAST:event_btnSendActionPerformed

    public void settext(String txt) {
        this.txtResponse.setText(txt);
    }
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSend;
    private javax.swing.JCheckBox ckRateXi;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel lblBad;
    private javax.swing.JLabel lblGood;
    private javax.swing.JSlider slRating;
    private javax.swing.JScrollPane spResponse;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextArea txtResponse;
    // End of variables declaration//GEN-END:variables
}
