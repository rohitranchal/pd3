package edu.purdue.absoa;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ABAuthorization {
	
	public static HashMap<String, List<String>> accessPolicies;
	
	public ABAuthorization() {
		accessPolicies = new HashMap<String, List<String>>();
		InputStream is;
		ABParser parser;
		HashMap<String, String> sMap;

		is = Thread.currentThread().getContextClassLoader().getResourceAsStream("ab.acl");
		if (is != null) {
			parser = new ABParser(is);
			sMap = parser.processLineByLine();
			for (HashMap.Entry<String, String> entry : sMap.entrySet()) {
			    String key = entry.getKey();
			    String value = entry.getValue();
			    String[] valList = value.split("\\s*,\\s*");
			    accessPolicies.put(key, Arrays.asList(valList));
			}
		}
	}
	
	public boolean authorize(String dataReq, byte[] certificate) {
		boolean result = false;
		try {
			InputStream bis = new ByteArrayInputStream(certificate);
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)certFactory.generateCertificate(bis);
			bis.close();
			String subject = cert.getSubjectX500Principal().getName().split(",")[0].split("=")[1];	
			String apn, policy, request;
			for(int i=0; i<accessPolicies.get(dataReq).size(); i++) {	
				apn = accessPolicies.get(dataReq).get(i);
				policy = "policy/policy-" + apn + ".xml";
				request = "request/request-" + apn + ".xml";
				HashMap<String, String> params = new HashMap<String, String>();
				params.put("#ABRESOURCE#", dataReq);
				params.put("#ABCLIENT#", subject);
				ABXACML abx = new ABXACML(policy, request, params);
				String res = abx.evaluate();
				if (res.contains("Permit")) {
					result = true;
				} else {
					return false;
				}
			}
		} catch(Exception e) {
			System.out.println("AB AZ authorization exception: " + e);
		}		
		return result;
	}
}
