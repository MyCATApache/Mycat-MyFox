package nioftpproxy;
public class ProxySettings
{
    private final int localPort;
    private final int remotePort;
    private final String remoteHost;

    public ProxySettings(int localPort, int remotePort, String remoteHost) {
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.remoteHost = remoteHost;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public String toString() {
        return "ProxySettings{" +
                "localPort=" + localPort +
                ", remotePort=" + remotePort +
                ", remoteHost='" + remoteHost + '\'' +
                '}';
    }
}
