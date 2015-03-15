package edu.purdue.absoa;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;

public class ABXACML {
	
	private static String policy;
	private static String request;
	
	public ABXACML(String pol, String req, HashMap<String, String> params) {
		InputStream policyStream = getClass().getClassLoader().getResourceAsStream(pol);
		InputStream reqStream = getClass().getClassLoader().getResourceAsStream(req);

		String polStr = streamToString(policyStream);
		StringBuffer sb = new StringBuffer(polStr);
		try {
			policy = writeToFile(sb);
		} catch (IOException e) {
			e.printStackTrace();
		}

		request = streamToString(reqStream);
		for (Map.Entry<String, String> entry : params.entrySet()) {
			request = request.replace(entry.getKey(), entry.getValue());
		}
	}
	
	public String evaluate() {
		PDP pdp = getPDPInstance(policy);
		String res = pdp.evaluate(request);
		ByteArrayInputStream in = new ByteArrayInputStream(res.getBytes());
		OMElement elem = OMXMLBuilderFactory.createOMBuilder(in).getDocumentElement();
		
		OMElement tmp = elem.getFirstChildWithName(new QName("urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", "Result"));
		tmp = tmp.getFirstChildWithName(new QName("urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", "Decision"));
		return tmp.getText();
	}
	
	private static PDP getPDPInstance(String policyLocation) {
		PolicyFinder finder= new PolicyFinder();
        Set<String> policyLocations = new HashSet<String>();
        policyLocations.add(policyLocation);
        FileBasedPolicyFinderModule testPolicyFinderModule = new FileBasedPolicyFinderModule(policyLocations);
        Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        policyModules.add(testPolicyFinderModule);
        finder.setModules(policyModules);

        Balana balana = Balana.getInstance();
        PDPConfig pdpConfig = balana.getPdpConfig();
        pdpConfig = new PDPConfig(pdpConfig.getAttributeFinder(), finder,
        pdpConfig.getResourceFinder(), true);
        return new PDP(pdpConfig);
	}

	private static String streamToString(java.io.InputStream is) {
		Scanner scanner = new Scanner(is);
		Scanner s = scanner.useDelimiter("\\A");
		String str = s.hasNext() ? s.next() : "";
		scanner.close();
		s.close();
		return str;
	}

	public static String writeToFile(StringBuffer sb) throws IOException {
		File tempFile = File.createTempFile("temp", ".tmp");
		FileWriter fileWriter = new FileWriter(tempFile, true);
		String tmpPath = tempFile.getAbsolutePath();
		BufferedWriter bw = new BufferedWriter(fileWriter);
		bw.write(sb.toString());
		bw.close();
		tempFile.deleteOnExit();
		return tmpPath;
	}	
}
