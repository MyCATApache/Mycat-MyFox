package nioftpproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

public class BackFPTCommandNIOHandler extends  AbstractFTPCmdHandler{
	protected static Logger logger = Logger.getLogger(BackFPTCommandNIOHandler.class);
	

	public BackFPTCommandNIOHandler(SelectionKey selectKey,SocketChannel channel,FTPSession ftpSession) throws IOException {
		super(selectKey,channel,ftpSession);
		ftpSession.serverCMDHandler=this;
	}

	

	@Override
	public void handleFTPCmd(String fromClient) throws IOException {
		
		String cmd = fromClient.toUpperCase();
		if(status==ST_INIT)
		{
			 
			this.answerSocket("USER "+this.ftpSession.clientUser+CRLF);
			status=ST_AUTHING;
			return;
		}else if(status==ST_AUTHING)
		{
			if(cmd.startsWith("230"))
		   {
				this.ftpSession.clientLogined=true;
				status=ST_CONNECTED;
		    }else if(cmd.startsWith("220"))
					{
		    	        logger.info("received ftp server welcoming message:"+cmd);
						return;
					}
			
			
		}else if(status==ST_CONNECTED)
		{
			if(cmd.startsWith("150"))
			{//opened data channel operation
				ftpSession.datachannelCmd=fromClient;
			}else
			if(cmd.startsWith("226"))
			{
				this.ftpSession.serverDataFinished=true;
				this.logSessionInfo("FTP data  success transfered ,server is "+this.ftpSession.serverIP);
			}
			else if(cmd.startsWith("227"))
			{//passvive cmd response,try to connect datasocket
				int serverDataPort=FTPUtil.parsePort(fromClient);
				 //≥¢ ‘¡¨Ω”Remote data socket
		         InetSocketAddress serverAddress = new InetSocketAddress(ftpSession.serverIP,serverDataPort);  
		         final SocketChannel remoteServerChannel = SocketChannel.open();
		         remoteServerChannel.configureBlocking(false);
		         remoteServerChannel.connect(serverAddress);
		         ftpSession.serverDataSocket=remoteServerChannel;
		         remoteServerChannel.register(this.ftpSession.nioSelector, SelectionKey.OP_CONNECT, ftpSession.proxyTransDataHandler);
		         this.logSessionInfo("Connecting to  ftp server for data trans " + ftpSession.serverIP + ":"+ serverDataPort);
		         return;
			}else if(cmd.startsWith("421"))
			{
				this.logSessionInfo(cmd);
			}
		}
		//proxy to client
		this.ftpSession.clientCMDHandler.answerSocket(fromClient+CRLF);
		
	}





	 
}
