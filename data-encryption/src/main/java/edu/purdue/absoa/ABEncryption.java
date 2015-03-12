package edu.purdue.absoa;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ABEncryption {
	
	private static HashMap<String, String> abData;
	private static SecretKeySpec skey;    				
	private static Cipher cipher;
	
	public ABEncryption() {
		abData = new HashMap<String, String>();
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("ab.data");
		ABDataParser parser = new ABDataParser(is);
		try {
			parser.processLineByLine();
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}
	
	private static String valueEncryption(String request, byte[] secret) {	
		try {
			skey = new SecretKeySpec(secret, "AES");    				
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		    cipher.init(Cipher.ENCRYPT_MODE, skey);
		    System.out.println("Symkey: " + new String(skey.getEncoded()));
		    byte[] ct = cipher.doFinal(request.getBytes("UTF-8"));
	    	Encoder encoder = java.util.Base64.getEncoder();
	    	String eVal = encoder.encodeToString(ct);
		    return eVal;
		} catch(Exception e){}
		return null;
	}
	
	public static void setABData(String abkey, String abvalue)
	{
		abData.put(abkey, abvalue);
	}

	public static String getABData(String abkey)
	{
		return abData.get(abkey);
	}
	
	public static void main(String[] args) {
		InputStream is;
		byte[] secBytes, secret;
		String cVal;
		OutputStream out;
		Writer writer;
		ABEncryption abe = new ABEncryption();
		try {
			for (HashMap.Entry<String, String> entry : abData.entrySet()) {
			    String key = entry.getKey();
			    String value = entry.getValue();
			    is = Thread.currentThread().getContextClassLoader().getResourceAsStream(key);
			    if (is != null) {
			    	secBytes = new byte[is.available()];
			    	is.read(secBytes);
			    	is.close();
			    	Decoder decoder = java.util.Base64.getDecoder();
			    	secret = decoder.decode(secBytes);
			    	cVal = valueEncryption(value, secret);
			    } else {
			    	cVal = value;
			    }			    
			    out = new FileOutputStream("src/main/resources/ab.cipher", true);
			    writer = new OutputStreamWriter(out);
			    writer.append(key + " = " + cVal + System.lineSeparator());
			    writer.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
