package nioftpproxy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

public class ProxyConfiguration {
	Properties properties;

	public int bindPort;
	public String bindAddress;
	public String publicIP;


	// variables read from configuration file
	boolean onlyAuto;
	String autoHostname;
	int autoPort;
	String masqueradeHostname;
	boolean isUrlSyntaxEnabled;
	boolean serverOneBindPort, clientOneBindPort;
	boolean validateDataConnection;
	boolean debug;


    int client_socket_timeout;
	// messages
	String msgConnect;
	String msgConnectionRefused;
	String msgOriginAccessDenied;
	String msgDestinationAccessDenied;
	String msgIncorrectSyntax;
	String msgInternalError;
	String msgMasqHostDNSError;
	
	//users binding ip configuration
	String ip_users;
	HashMap<String,UserAddress> userMap =new HashMap<String,UserAddress>();
	int buffer_size=2048;

	
	public void loadProperties(Properties properties) throws Exception {
		this.properties = properties;

		bindPort = getInt("bind_port", 8089);
		String ba = getString("bind_address");
		bindAddress = ba == null ? "0.0.0.0": ba;

		masqueradeHostname = getString("masquerade_host");
		if (masqueradeHostname != null) {
			// This is just to throw an UnknownHostException
			// if the config is incorrectly set up.
			InetAddress.getByName(masqueradeHostname.trim());
		}

		onlyAuto = getBool("only_auto", false);
		autoHostname = getString("auto_host");
		if (autoHostname != null) {
			autoHostname = autoHostname.trim();
		}
		autoPort = getInt("auto_port", 21);

		isUrlSyntaxEnabled = getBool("enable_url_syntax", true);
		validateDataConnection = getBool("validate_data_connection", true);
		debug = getBool("output_debug_info", false);
		client_socket_timeout =  getInt("client_socket_timeout",5000);
		//msgConnect = "220 " + getString("msg_connect", "FTP Server (glz license) ready.");
		msgConnect = "220 " + "FTP Server (glz license "+ba+") ready.";

		msgConnectionRefused = "421 " + getString("msg_connection_refused", "Connection refused, closing connection.");

		msgOriginAccessDenied = "531 " + getString("msg_origin_access_denied", "Access denied - closing connection.");

		msgDestinationAccessDenied = "531 " + getString("msg_destination_access_denied", "Access denied - closing connection.");

		msgIncorrectSyntax = "531 " + getString("msg_incorrect_syntax", "Incorrect usage - closing connection.");

		msgInternalError = "421 " + getString("msg_internal_error", "Internal error, closing connection.");

		msgMasqHostDNSError = "421 " + getString("msg_masqerade_hostname_dns_error", "Unable to resolve address for " + masqueradeHostname + " - closing connection.");
	
	    ip_users=getString("ip_users");
	    parseIpUsers(ip_users);
	    
	    buffer_size=getInt("buffer_size",2048);
        
	}

	private void parseIpUsers(String ip_users){
		userMap.clear();
		if(null==ip_users || ip_users.length()<1){
			return;
		}
		String[] alluserinfo =ip_users.split("/");
		for(int i=0;i<alluserinfo.length;i++){
			String tmpuser = alluserinfo[i];
			String[] ipportusers = tmpuser.split(":");
			String ip = ipportusers[0];
			int port = new Integer(ipportusers[1]).intValue();
			String users = ipportusers[2];
			String[] allusers = users.split(",");
			for(int j=0 ;j<allusers.length;j++){
				String username= allusers[j];
				UserAddress useradd = new UserAddress();
				useradd.setIp(ip);
				useradd.setPort(port);
				useradd.setUsername(username);
				userMap.put(username, useradd);
			}
		}
		
	}
	public String getIpbyUser(String username){
		String ip ="";
		UserAddress ua = userMap.get(username);
		if(null==ua){
			ip =autoHostname;
		}else{
			ip= ua.getIp();
		}
		return ip;
	}
	public int getPortbyUser(String username){
		int port ;
		UserAddress ua = userMap.get(username);
		if(null==ua){
			port =autoPort;
		}else{
			port = ua.getPort();
		}
		return port;
	}
	public boolean getBool(String name, boolean defaultValue) {
		String value = getString(name);
		return value == null ? defaultValue : value.trim().equals("1");
	}

	public int getInt(String name, int defaultValue) {
		String value = properties.getProperty(name);
		properties.remove(name);
		return value == null ? defaultValue : Integer.parseInt(value.trim());
	}

	public String getString(String name) {
		return getString(name, null);
	}

	public String getString(String name, String defaultValue) {
		String value = properties.getProperty(name, defaultValue);
		properties.remove(name);
		return value;
	}

	/**
	 * Returns an array of length 2n, where n is the number of port ranges
	 * specified. Index 2i will contain the first port number in the i'th range,
	 * and index 2i+1 will contain the last. E.g. the string
	 * "111,222-333,444-555,666" will result in the following array: {111, 111,
	 * 222, 333, 444, 555, 666, 666}
	 */
	public int[] getPortRanges(String name) {
		String s = getString(name);
		if (s == null)
			return null;

		StringTokenizer st = new StringTokenizer(s.trim(), ",");
		int[] ports = new int[st.countTokens() * 2];

		if (ports.length == 0)
			return null;

		int lastPort = 0;
		for (int p = 0; st.hasMoreTokens(); p += 2) {
			String range = st.nextToken().trim();
			int i = range.indexOf('-');

			if (i == -1) {
				ports[p] = ports[p + 1] = Integer.parseInt(range);
			} else {
				ports[p] = Integer.parseInt(range.substring(0, i));
				ports[p + 1] = Integer.parseInt(range.substring(i + 1));
			}
			if (ports[p] < lastPort || ports[p] > ports[p + 1]) {
				throw new RuntimeException("Ports should be listed in increasing order.");
			}
			lastPort = ports[p + 1];
		}

		return ports;
	}
	public void printInfo(){
		String CRLF = "\n";
		String msg="CONFIG:";
		StringBuffer buffer = new StringBuffer();
		buffer.append(msg+"bindPort="+bindPort + CRLF);
		buffer.append(msg+"bindAddress="+bindAddress + CRLF);
		buffer.append(msg+"serverOneBindPort="+serverOneBindPort + CRLF);
		buffer.append(msg+"clientOneBindPort="+clientOneBindPort + CRLF);
		buffer.append(msg+"masqueradeHostname="+masqueradeHostname + CRLF);
		buffer.append(msg+"onlyAuto="+onlyAuto + CRLF);
		buffer.append(msg+"isUrlSyntaxEnabled="+isUrlSyntaxEnabled + CRLF);
		buffer.append(msg+"validateDataConnection="+validateDataConnection + CRLF);
		buffer.append(msg+"debug="+debug + CRLF);
		buffer.append(msg+"ip_users="+ip_users + CRLF);
		buffer.append(msg+"userMap="+userMap + CRLF);
        
		System.out.println(buffer.toString());
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("proxy.conf"));
		} catch (IOException e) {
			e.printStackTrace();
		}
        try {
			ProxyConfiguration config = new ProxyConfiguration();
			config.loadProperties(properties);
			config.printInfo();
			System.out.println("user zhaoliu's ip="+config.getIpbyUser("zhaoliu")+" port="+config.getPortbyUser("zhaoliu"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

class UserAddress {
	private String username;
	private String ip;
	private int port;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
    
	public String toString(){
		return ip+":"+port;
	}
}
}
