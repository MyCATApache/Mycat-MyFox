package nioftpproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.ecip.NewFtpProxy;


public class FTPCommandNIOHandler extends  AbstractFTPCmdHandler {
	protected static Logger logger = Logger.getLogger(FTPCommandNIOHandler.class);
	public FTPCommandNIOHandler( SelectionKey selectKey,FTPSession  session)
			throws IOException {
		super(selectKey,session.clientCMDSocket,session);
		ftpSession.clientCMDHandler=this;
		ftpSession.proxyTransDataHandler=new ProxyTransDataNIOHandler(ftpSession);
		InetSocketAddress clientAddr = (InetSocketAddress) ftpSession.clientCMDSocket.getRemoteAddress();
		ftpSession.clientIP = clientAddr.getHostString();//.replace('.', ',');
		ftpSession.clientPort=clientAddr.getPort();
		answerSocket("220 Java FTP Proxy Server (usage: USERID=user@site) ready." + CRLF);

	}

	public void handleFTPCmd(String fromClient) throws IOException {
		String cmd = fromClient.toUpperCase();
		this.debugSessionInfo(client2proxy , fromClient );
         if (cmd.startsWith("USER")) {
			handlerUserCmd(fromClient);
		} else if (cmd.startsWith("FEAT")) {
			String resp = "211-no-features" + CRLF + "211 End" + CRLF;
			answerSocket(resp);
			return;
		} else if (cmd.startsWith("EPSV") || cmd.startsWith("EPRT")) {
			String resp = "500 not supported command " + fromClient + CRLF;
			answerSocket(resp);
			return;
		} else if ((cmd.startsWith("PASV"))||(cmd.startsWith("PORT")))
		  {
			if (!ftpSession.clientLogined) {
				answerSocket(MsgText.msgNotLoggedIn + CRLF);
				return;
			}
			 this.ftpSession.closeDataSocket();
			 this.ftpSession.proxyTransDataHandler=new ProxyTransDataNIOHandler(ftpSession); 
			if(cmd.startsWith("PASV"))
			{
				ftpSession.clientPassiveMode=true;
				//init passive data transfer Socket Server for clients
				ftpSession.clientDataServerSocket = ServerSocketChannel.open();
		        final InetSocketAddress isa2 = new InetSocketAddress(NewFtpProxy.PROXY_CONFIG.bindAddress,0);
		        ftpSession.clientDataServerSocket.bind(isa2);
		        
		        ftpSession.clientDataServerSocket.configureBlocking(false);
		        PassiveSocketServerHandler passiveDataHandler=new PassiveSocketServerHandler(ftpSession);
		        ftpSession.clientDataServerSocket.register(ftpSession.nioSelector, SelectionKey.OP_ACCEPT,passiveDataHandler);
			}else
			{
				ftpSession.clientPortModePort = FTPUtil.parsePort(fromClient);
				ftpSession.clientPassiveMode=false;
			}
			 //通知后端Server进入PASV模式,命令成功后，再告知前端
			 ftpSession.serverCMDHandler.answerSocket("PASV "+CRLF);
			 this.debugSessionInfo(" client data server socket opened ,waiting ftp server enter PASV mode ,then notify client ",null);

		}
		else if(cmd.startsWith("SYST"))
		{
			this.answerSocket("215 UNIX emulated by HP FTP-Proxy"+CRLF);
			return;
		}else
		{
			if(ftpSession.serverCMDHandler==null)
			{
				warnSessionInfo("server cmd handler is null ,can't process command,so close it " +fromClient);
				this.ftpSession.close("server cmd handler is null");
			}else
			{
			ftpSession.serverCMDHandler.answerSocket(fromClient+CRLF);
			}
		}
	}


	private void handlerUserCmd(String fromClient) throws IOException {
		 String userString = fromClient.substring(5);
         int a = userString.indexOf('@');
         int c = userString.lastIndexOf(':');
         String username=null;
         String hostname=null;
         int serverPort=21;
         if (a == -1) {
         	username = userString;
         	//使用Proxy代理的配置Server地址
         	hostname = NewFtpProxy.PROXY_CONFIG.getIpbyUser(username);
         	serverPort = NewFtpProxy.PROXY_CONFIG.getPortbyUser(username);
         } else if (c == -1) {
         	username = userString.substring(0, a);
         	hostname = userString.substring(a + 1);
         } else {
         	username = userString.substring(0, a);
         	hostname = userString.substring(a + 1, c);
         	serverPort = Integer.parseInt(userString.substring(c + 1));	
         }
         
         //if don't know which host to connect to
         if (hostname == null) {
        	 
        	 this.logSessionInfo(" hostname is null,can't process");
        	 answerSocket("500 hostname is null,can't process "+ CRLF);
             return;
         } 
         ftpSession.clientUser=username;
         this.ftpSession.serverIP=hostname;
         this.ftpSession.serverPort=serverPort;
         try
         {
         //尝试连接Remote Server
         InetSocketAddress serverAddress = new InetSocketAddress(hostname,serverPort);  
         final SocketChannel remoteServerChannel = SocketChannel.open();
         remoteServerChannel.configureBlocking(false);
         remoteServerChannel.connect(serverAddress);
         this.ftpSession.serverCMDSocket=remoteServerChannel;
         SelectionKey selectKey=remoteServerChannel.register(this.ftpSession.nioSelector, SelectionKey.OP_CONNECT, null);
         BackFPTCommandNIOHandler backCmdNIOHandler=new BackFPTCommandNIOHandler(selectKey,remoteServerChannel,this.ftpSession);
         selectKey.attach(backCmdNIOHandler);
         logger.info("Connecting to ftp server passive port for data trans " + hostname + ":" + serverPort  );
         }catch(Exception e)
         {
        	 warnSessionInfo("Connect  to ftp server failed ",e);
        	 answerSocket("500 Bad remote ftp server addr "+e.getMessage()+ CRLF);
        	 return;
         }
        
	}

	protected void closeSocket(String message) {
		if(ftpSession.clientUser==null)
		{
			this.debugSessionInfo(message,null);
			return;
		}
		this.ftpSession.close(message);
	}


}
