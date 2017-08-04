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
 * 客戶端是否登录成功
 */
public boolean clientLogined;
/**
 * 客户端用来传输FTP命令的Socket通道
 */
public SocketChannel clientCMDSocket;
public AbstractFTPCmdHandler clientCMDHandler;
/**
 * Proxy与Server建立的用来传输FTP命令的Socket通道
 * 
 */
public SocketChannel serverCMDSocket;
public AbstractFTPCmdHandler serverCMDHandler;
	/**
	 *  客户端用PASSIVE模式连接的时候，Proxy打開一個ServerSocket用來監聽請求数据传输的請求
	 */
public ServerSocketChannel clientDataServerSocket;

/**
 *  客户端來传输数据的Socket
 */
public SocketChannel clientDataSocket;
/**
 * proxy本身用PASSIVE模式去连接FPT Server，并用来传输数据的Socket通道
 */
public SocketChannel serverDataSocket;

/**
 * 用来绑定clientDataSocket与serverDataSocket，透传数据到对方的NIOHandler;
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
