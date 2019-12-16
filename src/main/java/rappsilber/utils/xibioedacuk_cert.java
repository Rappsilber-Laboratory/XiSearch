/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Mostly copied from stackoverflow
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class xibioedacuk_cert {

    static String cert = "-----BEGIN CERTIFICATE-----\n"
            + "MIIH+DCCBeCgAwIBAgIUT3AXLSg/c9JESUA/xgzjVKsGedcwDQYJKoZIhvcNAQEL\n"
            + "BQAwSTELMAkGA1UEBhMCQk0xGTAXBgNVBAoMEFF1b1ZhZGlzIExpbWl0ZWQxHzAd\n"
            + "BgNVBAMMFlF1b1ZhZGlzIEVWIFNTTCBJQ0EgRzMwHhcNMTgxMTE5MTU0OTQyWhcN\n"
            + "MjAxMTE5MTU1OTAwWjCB6TETMBEGCysGAQQBgjc8AgEDEwJHQjEaMBgGA1UEDwwR\n"
            + "R292ZXJubWVudCBFbnRpdHkxGjAYBgNVBAUTEUdvdmVybm1lbnQgRW50aXR5MQsw\n"
            + "CQYDVQQGEwJHQjEaMBgGA1UECAwRQ2l0eSBvZiBFZGluYnVyZ2gxEjAQBgNVBAcM\n"
            + "CUVESU5CVVJHSDEgMB4GA1UECgwXVW5pdmVyc2l0eSBvZiBFZGluYnVyZ2gxIDAe\n"
            + "BgNVBAsMF1VuaXZlcnNpdHkgb2YgRWRpbmJ1cmdoMRkwFwYDVQQDDBB4aTMuYmlv\n"
            + "LmVkLmFjLnVrMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9aXEkdom\n"
            + "nL6tMl7oYZIwj+YKAxmXabZdp2TlrVLI6sM1DilYjuy0DAmUITLZX1YfBlhSKh4o\n"
            + "c1wxJCs0z8J+8iz8ACnx2pxaKFR7SUAjEWuFEmEthsz2+qztfsyVSqkf90DUctkM\n"
            + "JGyTGv57Dg8k4y9awGuPVnDMaWRYo+5qrtiKBYlCrmEK6RuDfDIx7Bl9Y8rL+Jbo\n"
            + "lCRwW6T3iZLtso7kOU0G9Z4obOCaKMhZGGVdarmh/cbg3iDubZKvcF7DaWaIfRA3\n"
            + "OivbEXSrUw5gatTqHcTfYesA7hkCMlaTduuf2PzQrxLiAqT3qoZWWgKYdjp9+/Yk\n"
            + "mIedVSln9Meh4wIDAQABo4IDNTCCAzEwDAYDVR0TAQH/BAIwADAfBgNVHSMEGDAW\n"
            + "gBTlhFTQkEmfOLryyeEqCMVOn6BIPzB4BggrBgEFBQcBAQRsMGowOQYIKwYBBQUH\n"
            + "MAKGLWh0dHA6Ly90cnVzdC5xdW92YWRpc2dsb2JhbC5jb20vcXZldnNzbGczLmNy\n"
            + "dDAtBggrBgEFBQcwAYYhaHR0cDovL2V2Lm9jc3AucXVvdmFkaXNnbG9iYWwuY29t\n"
            + "MBsGA1UdEQQUMBKCEHhpMy5iaW8uZWQuYWMudWswWgYDVR0gBFMwUTBGBgwrBgEE\n"
            + "Ab5YAAJkAQIwNjA0BggrBgEFBQcCARYoaHR0cDovL3d3dy5xdW92YWRpc2dsb2Jh\n"
            + "bC5jb20vcmVwb3NpdG9yeTAHBgVngQwBATAdBgNVHSUEFjAUBggrBgEFBQcDAgYI\n"
            + "KwYBBQUHAwEwPAYDVR0fBDUwMzAxoC+gLYYraHR0cDovL2NybC5xdW92YWRpc2ds\n"
            + "b2JhbC5jb20vcXZldnNzbGczLmNybDAdBgNVHQ4EFgQUmnRldauhbIeyMDj90ub1\n"
            + "xSyU4PUwDgYDVR0PAQH/BAQDAgWgMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkA\n"
            + "dwC72d+8H4pxtZOUI5eqkntHOFeVCqtS6BqQlmQ2jh7RhQAAAWcssu3dAAAEAwBI\n"
            + "MEYCIQC/kdoAHszzaTsA56T6CxVSd0afBUYSkzEc2cENVVgnEAIhAMjrJiFfM0zX\n"
            + "f/CWfwA1D68+bso0VP6FOebt4Vqm8Zh4AHYApLkJkLQYWBSHuxOizGdwCjw1mAT5\n"
            + "G9+443fNDsgN3BAAAAFnLLLuBgAABAMARzBFAiBWWVUsQXolG/sdm7nadhSvfIW3\n"
            + "aKIriNrgx12J0ud6hwIhAJue7hJnZ1IOyaZJFgxwW/YNHO8a+aE4gZmtW3HvEVwW\n"
            + "AHYAVYHUwhaQNgFK6gubVzxT8MDkOHhwJQgXL6OqHQcT0wwAAAFnLLLuCAAABAMA\n"
            + "RzBFAiEA/g2lj7cDAUF57VaID6BxEwh3L+yaBquWCTp9MiJTJHwCIAauCRqnWiCf\n"
            + "FZndDiC00tfqqrYuUExj+VSMWnwhZ+t/MA0GCSqGSIb3DQEBCwUAA4ICAQATBve5\n"
            + "zRMRWAiLu7GFj119TCjU573pHshrN+0MMq2UGp6y6sYM4LjQGBIiKJ4SZg3QKcUs\n"
            + "n27R/D4T4D8LH1oupiYFBmug5+QDKBkduy/0qjchfsn8VwPJVEQZAl+j6dCzBHxF\n"
            + "tSxj8AKgmALfW8zy/G7bGnVVOAyZh+s7mbMDsPGAXp8XEMGoFYEnkdnZyGn7e7mc\n"
            + "NbO8wTz3lGqA3Q0DDtgtquyyqJylNTdVAR2A5e0ZridxJ5WdxAR5dA1rdo4X5hac\n"
            + "klT8t/9GANuBizuy1Fcx4c9U+RHYbSBPyudJSAL+/jphx+c4BM/f9NfOhTc/97Ya\n"
            + "ALDa+M+A35K2uu4CMLTLUNCDkA/1wyPxOrL/73ZWI3SYw8DeVAdrxdZvJeWLZ4bh\n"
            + "A0gg1rYbI7KQhaz1MTlR0yBMFAY0zF2XA9XrdWGKgT2i/di7QbIgoAqtk6vnctU0\n"
            + "FjlmC9xZpofCcIbffC/ZOcTHeuwxd9CgHiQFo+FhujZdyDcux/EMb2t9M29prwsZ\n"
            + "m7Ze9wrLwMrsF5XzvR0cVDlYgOo0bsfFxfJqberjg7l7yCweT24nS5unU+6uyHD1\n"
            + "CiaeSfndVDBJolEklfXr658XPOkc6nl5i7HdqXbXT74NjyNyFrWg9A95Gf/nAqEV\n"
            + "L+B4jrGfTU7Zu5AtQb6dA82IeFOxLkED26ln5A==\n"
            + "-----END CERTIFICATE-----\n";

    /**
     * This method creates a new SSLContext that accepts the certificate for the xi3.bio.ed.ac.uk server
     * @return
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException 
     */
    public static SSLContext getXi3SSLContext() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        // convert the certificate into a byte stream for reading
        ByteArrayInputStream certInputStream = new ByteArrayInputStream(cert.getBytes());
        // get a new instance of an X.509 certificateFactory
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        // "read in" the certificate
        X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(certInputStream);
        String alias = "letsencrypt-xi3";

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        trustStore.setCertificateEntry(alias, cert);
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(trustStore, null);
        KeyManager[] keyManagers = kmf.getKeyManagers();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(trustStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext;
        
    }

}
