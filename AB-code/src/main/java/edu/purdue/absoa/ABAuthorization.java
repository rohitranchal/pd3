package edu.purdue.absoa;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ABAuthorization {
	
	public static List<HashMap<String, List<String>>> authSubjects;
	
	public ABAuthorization() {
		authSubjects = new ArrayList<HashMap<String, List<String>>>();
		InputStream is;
		ABParser parser;
		HashMap<String, String> sMap;
		for(int i=0;;i++) {
			is = Thread.currentThread().getContextClassLoader().getResourceAsStream("policy/policy-" + i);
			if (is != null) {
				authSubjects.add(i, new HashMap<String, List<String>>());
				parser = new ABParser(is);
				sMap = parser.processLineByLine();
				for (HashMap.Entry<String, String> entry : sMap.entrySet()) {
				    String key = entry.getKey();
				    String value = entry.getValue();
				    String[] valList = value.split("\\s*,\\s*");
				    authSubjects.get(i).put(key, Arrays.asList(valList));
				}
			} else {
				break;
			}
		}
	}
	
	public boolean authorize(String request, byte[] certificate) {
		try {
			InputStream bis = new ByteArrayInputStream(certificate);
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)certFactory.generateCertificate(bis);
			bis.close();

			String sub = cert.getSubjectX500Principal().getName().split(",")[0].split("=")[1];
			for(int i=0; i<authSubjects.size(); i++) {				
				if (authSubjects.get(i).get(request).contains(sub) ) {
					return true;
				}
			}
		} catch(Exception e) {
			System.out.println("AB AZ authorization exception: " + e);
		}
		
		return false;
	}
}
