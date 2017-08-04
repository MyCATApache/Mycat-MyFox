package nioftpproxy;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

public class FTPSession {
	protected static Logger logger = Logger.getLogger(FTPSession.class);	
public String clientIP;	
public int clientPort;
public int clientPortModePort;
public String publicIP;
public String serverIP;
public int serverPort=21;
public String clientUser;
public boolean clientPassiveMode=false;
public Selector nioSelector;
public BufferPool bufPool;
/**
 * �͑����Ƿ��¼�ɹ�
 */
public boolean clientLogined;
/**
 * �ͻ�����������FTP�����Socketͨ��
 */
public SocketChannel clientCMDSocket;
public AbstractFTPCmdHandler clientCMDHandler;
/**
 * Proxy��Server��������������FTP�����Socketͨ��
 * 
 */
public SocketChannel serverCMDSocket;
public AbstractFTPCmdHandler serverCMDHandler;
	/**
	 *  �ͻ�����PASSIVEģʽ���ӵ�ʱ��Proxy���_һ��ServerSocket�Á�O Ո�����ݴ����Ո��
	 */
public ServerSocketChannel clientDataServerSocket;

/**
 *  �ͻ��ˁ������ݵ�Socket
 */
public SocketChannel clientDataSocket;
/**
 * proxy������PASSIVEģʽȥ����FPT Server���������������ݵ�Socketͨ��
 */
public SocketChannel serverDataSocket;

/**
 * ������clientDataSocket��serverDataSocket��͸�����ݵ��Է���NIOHandler;
 */
public ProxyTransDataNIOHandler proxyTransDataHandler;
public boolean clientDataFinished;
public boolean serverDataFinished;
public String datachannelCmd;

public void close(String messag) {
	    logger.info("close session "+this.clientDebugInfo()+ " for reason "+messag);
		closeSocket(clientCMDSocket);
		clientCMDHandler.releaseNIOBuffer();
		closeSocket(serverCMDSocket);
		serverCMDHandler.releaseNIOBuffer();
		closeDataSocket();
		
		
		
		 
	
}
private void closeSocket(Channel channel)
{
	if(channel!=null)
	{
		try {
			channel.close();
		} catch (IOException e) {
			//
		}
		
	}
}
public String clientDebugInfo()
{
	return this.clientUser+'@'+clientIP+':'+this.clientPort+' ';
}
public void closeDataSocket() {
	closeSocket(clientDataServerSocket);
	closeSocket(clientDataSocket);
	closeSocket(serverDataSocket); 
	proxyTransDataHandler.releaseNIOBuffer();
}
}
