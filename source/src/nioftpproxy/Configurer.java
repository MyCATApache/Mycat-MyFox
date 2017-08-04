package nioftpproxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Loads proxy settings from the property file
 */
public class Configurer {
    private final static String CONFIGURATION_FILE = "proxy.properties";
    private final static String LOCAL_PORT_PROPERTY = ".localPort";
    private final static String REMOTE_PORT_PROPERTY = ".remotePort";
    private final static String REMOTE_HOST_PROPERTY = ".remoteHost";

    public static Collection<ProxySettings> readConfiguration() throws IOException {
        final Collection<ProxySettings> proxyTaskSettingses = new ArrayList<ProxySettings>();
        final Properties properties = loadProperties();
        final Collection<String> proxyNames = collectProxyNames(properties);
        for (String proxyName : proxyNames) {
            proxyTaskSettingses.add(createProxyTask(properties, proxyName));
        }
        return proxyTaskSettingses;
    }

    private static Collection<String> collectProxyNames(final Properties prop) {
       final Set<String> propertiesName = new HashSet<String>();
        for (String name : prop.stringPropertyNames()) {
            propertiesName.add(name.substring(0, name.indexOf(".")));
        }
        return propertiesName;
    }

    private static Properties loadProperties() throws IOException {
        final Properties properties = new Properties();
        final InputStream stream = Configurer.class.getResourceAsStream("/"+CONFIGURATION_FILE);
        properties.load(stream);
        return properties;
    }

    private static ProxySettings createProxyTask(final Properties properties, final String proxyName) {
        final String hostName = properties.getProperty(proxyName + REMOTE_HOST_PROPERTY);
        final int localPort = Integer.parseInt(properties.getProperty(proxyName + LOCAL_PORT_PROPERTY));
        final int remotePort = Integer.parseInt(properties.getProperty(proxyName + REMOTE_PORT_PROPERTY));
        return new ProxySettings(localPort, remotePort, hostName);
    }
}
