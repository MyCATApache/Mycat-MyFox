package com.ecip;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import nioftpproxy.FTPNIOAcceptor;
import nioftpproxy.NIOReactorThread;
import nioftpproxy.ProxyConfiguration;
public class NewFtpProxy {
	final static Logger logger = Logger.getLogger(NewFtpProxy.class);
	private final static String defaultConfigFile = "proxy.conf";
	public final static ProxyConfiguration PROXY_CONFIG=new ProxyConfiguration();
	public static NIOReactorThread[] nioThreads;
    public static void startServer() throws IOException {
    	 new NIOReactorThread().start();
    }

    public static void main(String[] args) throws IOException {
    	
    	Map<String,String> commandLineArguments = new HashMap<>(args.length);
		for (int i = 0; i < args.length; i++) {
			int j = args[i].indexOf("=");
			if (j == -1) {
				logger.error("Invalid argument: " + args[i]);
				System.exit(0);
			}

			String name = args[i].substring(0, j);
			String value = args[i].substring(j + 1);

			if (commandLineArguments.containsKey(name)) {
				logger.error("Parameter error: --" + name + " may only be specified once.");
				System.exit(0);
			}

			commandLineArguments.put(name, value);
		}

		// read configuration
		String configFile = (String) commandLineArguments.get("config_file");
		if (configFile == null) {
			configFile = defaultConfigFile;
		}
		commandLineArguments.remove("config_file");

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(configFile));
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Configuration file error: " + e.getMessage());
			System.exit(0);
		}

		// command line arguments override those in the config file
		properties.putAll(commandLineArguments);

		
		try {
			PROXY_CONFIG.loadProperties(properties);
			PROXY_CONFIG.printInfo();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Invalid configuration: " + e.getMessage());
			System.exit(0);
			return; // to make it compile
		}
		int cpus=Integer.valueOf(properties.getProperty("nio_threads", Runtime.getRuntime().availableProcessors()+"")) ;
		nioThreads=new NIOReactorThread[cpus];
		for(int i=0;i<cpus;i++)
		{
			NIOReactorThread thread=new NIOReactorThread();
			thread.setName("NIO_Thread "+(i+1));
			thread.start();
			nioThreads[i]=thread;
		}
		//Æô¶¯NIO Acceptor
		new FTPNIOAcceptor().start();
		
    }
}
