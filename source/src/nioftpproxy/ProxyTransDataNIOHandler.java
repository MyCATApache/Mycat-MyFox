
package nioftpproxy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import nioftpproxy.ProxyBuffer.BufferState;

/**
 * Handler for client/server communications.
 */
public class ProxyTransDataNIOHandler implements NIOHandler{

	protected static Logger logger = Logger.getLogger(ProxyTransDataNIOHandler.class);
    private ProxyBuffer clientBuffer ;
    private ProxyBuffer serverBuffer ;
    private FTPSession ftpSession;
    public ProxyTransDataNIOHandler(FTPSession ftpSession) {
       this.ftpSession=ftpSession;
       clientBuffer=new ProxyBuffer(ftpSession.bufPool.allocByteBuffer());
       serverBuffer=new ProxyBuffer(ftpSession.bufPool.allocByteBuffer());
    }
    public void onConnected(SelectionKey key) throws IOException
    {
    	SocketChannel curChannel=(SocketChannel) key.channel();
    	//如果正在连接，则完成连接
        if(curChannel.isConnectionPending()){
       	 curChannel.finishConnect();
        }
        //连接成功后，注册接收服务器消息的事件
         curChannel.register(this.ftpSession.nioSelector, SelectionKey.OP_READ,this);
    	 if(curChannel==ftpSession.clientDataSocket)
    	 {
    		 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession," client DataSocket connected ");
    		 if(!ftpSession.clientPassiveMode)
             {
    			 //通知客户端,Port模式成功建立
    			 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession," Port Connect  to client success  ");
    			 ftpSession.clientCMDHandler.answerSocket("200 PORT command successful."+CRLF);
    			 return;
             }
    	 }else
    	 { // ftp server 's data tranport connected 
    		 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession," Server DataSocket connected ");
    		 if(ftpSession.clientPassiveMode)
             {
             //通知前端ＰＡＡＳＩＶＥ模式建立成功
    		 int dataServerPort=((InetSocketAddress)ftpSession.clientDataServerSocket.getLocalAddress()).getPort();
    		 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession," Success opened all DataSocket for client using PASV mode to trans data ");
      		  String toClient = "227 Entering Passive Mode (" + ftpSession.publicIP + ","
    		 + (int) (dataServerPort / 256) + ","
    		 + (dataServerPort % 256) + ")";
    		 ftpSession.clientCMDHandler.answerSocket(toClient+CRLF);
             }else
             {//PORT mode
            	 //打开客户端Port连接
            	 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession," try to connect Client data port for data trans  ");
            	  try
	                 {
	                 //尝试连接Client Port端口
	                 InetSocketAddress serverAddress = new InetSocketAddress(ftpSession.clientIP,ftpSession.clientPortModePort);  
	                 final SocketChannel remoteServerChannel = SocketChannel.open();
	                 remoteServerChannel.configureBlocking(false);
	                 remoteServerChannel.connect(serverAddress);
	                 this.ftpSession.clientDataSocket=remoteServerChannel;
	                 SelectionKey selectKey=remoteServerChannel.register(this.ftpSession.nioSelector, SelectionKey.OP_CONNECT, null);
	                 selectKey.attach(ftpSession.proxyTransDataHandler);
	                 logger.info("Connecting to client port for data trans  " + ftpSession.clientIP + ":" + ftpSession.clientPortModePort);
	                 }catch(Exception e)
	                 {
	                	 AbstractFTPCmdHandler.warnSessionInfo(logger, ftpSession,"Port Connect  to client  failed ",e.toString());
	                	 ftpSession.clientCMDHandler.answerSocket(MsgText.msgPortFailed + CRLF);
	                	 return;
	                 }
             }
    		 
    	 }
    	
         
    }
    public void handIO(SelectionKey selectionKey) {
    	SocketChannel curChannel=(SocketChannel) selectionKey.channel();
    	ProxyBuffer curChannelBuffer;
    	ProxyBuffer otherChannelBuffer;
    	SocketChannel otherChannel;
    	if(curChannel==ftpSession.clientDataSocket)
    	{
    		curChannelBuffer=this.clientBuffer;
    		otherChannelBuffer=this.serverBuffer;
    		otherChannel=ftpSession.serverDataSocket;
    	}
    		else{
    		curChannelBuffer=this.serverBuffer;
    		otherChannelBuffer=this.clientBuffer;
    		otherChannel=ftpSession.clientDataSocket;
    	}
    	try {
            if (selectionKey.isReadable()) 
            {
            	//读入到另外一个Channel的Buffer，等待写入
            	boolean socketDataEnd=readTo(curChannel,otherChannelBuffer);
            	if(socketDataEnd)
            	{
            	    if(otherChannelBuffer.isReadyToWrite())
            	    {//表明没有数据可传输，数据已经传输完成
            	    	 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession,"close data peer socket ,for data trans finished ."+getDataChannelOptInf());
            	    	NIOReactorThread.closeQuietly(otherChannel);
            	    }
            		return;
            	}
            	if (otherChannelBuffer.isReadyToRead()) register();
    	    }
            if (selectionKey.isWritable())
            	{
            	ByteBuffer buffer=curChannelBuffer.getBuffer();
            	curChannel.write(buffer);
                if (buffer.remaining() == 0) {
                	if(!otherChannel.isOpen())
                	{
                		NIOReactorThread.closeQuietly(curChannel);
                		 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession," close this socket ,for peer data transfer socket is closed. "+getDataChannelOptInf());
                		 return; 
                	}
                    buffer.clear();
                    curChannelBuffer.setState(BufferState.READY_TO_WRITE);
                }
                if (curChannelBuffer.isReadyToWrite()) register();
            	}
         } catch (final Exception exception) {
        	 if(exception instanceof IOException)
        	 {
        		 AbstractFTPCmdHandler.warnSessionInfo(logger, ftpSession,"ProxyTransDataNIOHandler handle IO error ",exception.getMessage()); 
        	 }else
        	 {
        		 AbstractFTPCmdHandler.warnSessionInfo(logger, ftpSession,"ProxyTransDataNIOHandler handle IO error ",exception); 
        	 }
        	
        	 ftpSession.closeDataSocket();
        }
    	
    }
    
    private String getDataChannelOptInf()
    {
    	return this.ftpSession.serverDataFinished?"Data channel operation success ":" Data channel operation failed ,server "+this.ftpSession.serverIP+(ftpSession.clientPassiveMode?" PASV":" PORT")+" mode,related :"+ftpSession.datachannelCmd;
    	
    }

    private boolean readTo(SocketChannel channel,ProxyBuffer proxBuffer) throws IOException {
    	ByteBuffer buffer=proxBuffer.getBuffer();
        int read = channel.read(buffer);
        if (read == -1) 
        	{
        	 if(channel==ftpSession.serverDataSocket)
        	 {
        		 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession," server data transfer channel closed."+getDataChannelOptInf());
        	 }else
        	 {
        		 AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession," client data transfer channel closed."+getDataChannelOptInf());
        		 NIOReactorThread.closeQuietly(ftpSession.clientDataServerSocket);
        	 }
        	 NIOReactorThread.closeQuietly(channel);
        	 return true;
        	}

        else if(read > 0) {
            buffer.flip();
            proxBuffer.setState(BufferState.READY_TO_READ);
        }
        return false;
    }
    
    private void register() throws ClosedChannelException {
    	if(ftpSession.clientDataSocket!=null && ftpSession.clientDataSocket.isOpen())
    	{
        int clientOps = 0;
        if (serverBuffer.isReadyToWrite()) clientOps |= SelectionKey.OP_READ;
        if (clientBuffer.isReadyToRead()) clientOps |= SelectionKey.OP_WRITE;
        ftpSession.clientDataSocket.register(ftpSession.nioSelector, clientOps, this);
    	}
    	if(ftpSession.serverDataSocket!=null && ftpSession.serverDataSocket.isOpen())
    	{
        int serverOps = 0;
        if (clientBuffer.isReadyToWrite()) serverOps |= SelectionKey.OP_READ;
        if (serverBuffer.isReadyToRead()) serverOps |= SelectionKey.OP_WRITE;
        ftpSession.serverDataSocket.register(ftpSession.nioSelector, serverOps, this);
    	}
    }
	@Override
	public void releaseNIOBuffer() {
		if(clientBuffer!=null)
		{
		this.ftpSession.bufPool.recycleBuf(clientBuffer.getBuffer());
		clientBuffer=null;
		}if(serverBuffer!=null)
		{
		this.ftpSession.bufPool.recycleBuf(serverBuffer.getBuffer());
		serverBuffer=null;
		}
	}


}
