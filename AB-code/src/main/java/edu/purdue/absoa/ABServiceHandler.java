package edu.purdue.absoa;

import java.io.InputStream;
import java.util.Base64.Decoder;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.thrift.TException;

public class ABServiceHandler implements ABService.Iface {
	
	private static HashMap<String, String> abData;
	private static HashMap<String, String> abKey;
	ABVerification abv;
	private static SecretKeySpec skey;    				
	private static Cipher cipher;

	public ABServiceHandler() {
		abData = new HashMap<String, String>();
		abKey = new HashMap<String, String>();
		try {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("ab.cipher");
			ABParser parser = new ABParser(is);
			abData = new HashMap<String, String>(parser.processLineByLine());
			is = Thread.currentThread().getContextClassLoader().getResourceAsStream("ab.key");
			parser = new ABParser(is);
			abKey = new HashMap<String, String>(parser.processLineByLine());
			is.close();
			abv = new ABVerification();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getValue(String request, String signature, String certificate) throws TException {
    	Decoder decoder = java.util.Base64.getDecoder();
    	byte[] sig = decoder.decode(signature);
    	byte[] cert = decoder.decode(certificate);
    	if (abv.verify(request, sig, cert)) {
    		return disseminate(request);    		    		
    	} else {
    		return null;
    	}
    }
	
	private static String disseminate(String request) {	
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    	Decoder decoder = java.util.Base64.getDecoder();
	    	byte[] ct = decoder.decode(abData.get(request));
	    	byte[] bkey = decoder.decode(abKey.get(request));
	    	skey = new SecretKeySpec(bkey, 0, bkey.length, "AES");
		    cipher.init(Cipher.DECRYPT_MODE, skey);
		    byte[] pt = cipher.doFinal(ct);
		    return new String(pt, "UTF-8");
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getABData(String abkey)
	{
		return abData.get(abkey);
	}
}
