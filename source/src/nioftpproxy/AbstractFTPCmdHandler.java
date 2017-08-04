package nioftpproxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

public abstract class AbstractFTPCmdHandler implements NIOHandler {
	protected static Logger logger= Logger.getLogger(AbstractFTPCmdHandler.class); ;
	protected SelectionKey selectKey;
	protected SocketChannel channel;
	public static final int ST_INIT=0;
	public static final int ST_AUTHING=1;
	public static final int ST_CONNECTED=2;
	protected int status=ST_INIT;
	protected FTPSession ftpSession;
	protected ContinueReadByteBuffer readBuffer;
	protected ByteBuffer writeBuffer;
	protected ByteBuffer originWriteBuffer;
	protected final static byte[] CRLF_BYTS = "\r\n".getBytes();
	

	// constants for debug output
	public static String server2proxy = "S->P: ";
	public static String proxy2server = "P->S: ";
	public static String proxy2client = "P->C: ";
	public static String client2proxy = "C->P: ";
	public static String server2client = "S->C: ";
	public static String client2server = "C->S : ";

	

	public AbstractFTPCmdHandler(SelectionKey key,SocketChannel channel,FTPSession ftpSession)
			throws IOException {
		this.selectKey=key;
		this.channel=channel;
		this.ftpSession=ftpSession;
		this.readBuffer = new ContinueReadByteBuffer(ftpSession.bufPool.allocByteBuffer());
		this.writeBuffer = ftpSession.bufPool.allocByteBuffer();
		writeBuffer.position(writeBuffer.limit());
		originWriteBuffer =  writeBuffer;
		
	}

	@Override
	public void handIO(SelectionKey key) throws IOException {
		try
		{
		if (key.isReadable()) {
			readFromSocket();
		} else if (key.isWritable()){
			writeToSocket();
		}
		}
		catch(Exception e)
		{
			logger.warn(" Handle IO err ,so close ftp session ",e);
			ftpSession.close("error:"+e.getLocalizedMessage());
		}

	}

	protected void writeToSocket() throws IOException {
		int writed=channel.write(writeBuffer);
		if (writeBuffer.hasRemaining()) {
			logger.warn("warning not write finished ,next to write ,writed "+writed +",remains"+ writeBuffer.remaining());
			selectKey.interestOps(selectKey.interestOps()|SelectionKey.OP_WRITE);
           
		}else
		{selectKey.interestOps(selectKey.interestOps()&~SelectionKey.OP_WRITE);
		}
	}

	public void answerSocket(String msg) throws IOException {
		this.debugSessionInfo( "answer request: ",msg);
		byte[] bytes = msg.getBytes("UTF-8");
		if (writeBuffer.position()==writeBuffer.limit()) {
			writeBuffer.clear();
			writeBuffer.put(bytes);
			writeBuffer.flip();
		} else if (writeBuffer.capacity() - writeBuffer.limit() < bytes.length) {
			writeBuffer.compact();
			if (writeBuffer.capacity() - writeBuffer.limit() < bytes.length) {
				logger.warn("warning writebuffer too small ,tobe writed data len:" + bytes.length
						+ " buf remains " + (writeBuffer.capacity() - writeBuffer.limit()));
				ByteBuffer tempwriteBuffer = ByteBuffer.allocate(writeBuffer.limit() + bytes.length);
				tempwriteBuffer.put(writeBuffer);
				tempwriteBuffer.put(bytes);
				writeBuffer = tempwriteBuffer;
				writeBuffer.flip();
			} else {
				int posi = writeBuffer.position();
				int limit = writeBuffer.limit();
				writeBuffer.limit(writeBuffer.capacity());
				writeBuffer.position(limit);
				writeBuffer.put(bytes);
				writeBuffer.limit(writeBuffer.position());
				writeBuffer.position(posi);
			}
		}
		writeToSocket();

	}

	protected void readFromSocket() throws IOException {

		int readLength = readBuffer.read(channel);
		if (readLength < 0) {
			closeSocket("socket read closed "+this.channel);
		} else {
			while(true)
			{
			int linePos = readBuffer.findBytesTwo(CRLF_BYTS);
			if (linePos==-1) {
				break;
			} else {
				String line = readBuffer.getString(0, linePos);
				readBuffer.setStartPos(readBuffer.getStartPos()+linePos + CRLF_BYTS.length);
				//this.debugSessionInfo("read:"+line+"  from socket channel:"+this.channel);
				handleFTPCmd(line);
			}
			}
		}

	}

	public abstract void handleFTPCmd(String fromClient) throws IOException;

	

	protected void closeSocket(String message) {
		selectKey.cancel();
		this.ftpSession.close(message);
	}

	@Override
	public void onConnected(SelectionKey key) throws IOException {
		logger.info("connection connected to me "+key.channel());
      //如果正在连接，则完成连接
        if(channel.isConnectionPending()){
            channel.finishConnect();
        }
        //连接成功后，注册接收服务器消息的事件
        key.interestOps(SelectionKey.OP_READ);
       
	}
	
	public void debugSessionInfo(String message1,String message2)
	{
		debugSessionInfo(logger,ftpSession,message1,message2);
	}
	public void logSessionInfo(String message)
	{
		logSessionInfo(logger,this.ftpSession,message);
	}
	
	public void warnSessionInfo(String message,Throwable e)
	{
		logger.warn(this.ftpSession.clientDebugInfo(),e);
	}

	public void warnSessionInfo(String message)
	{
		logger.warn(this.ftpSession.clientDebugInfo()+message);
	}
	public static void debugSessionInfo(Logger logger,FTPSession ftpSession,String message1,String message2)
	{
		if(logger.isDebugEnabled())
		{
			logger.debug(ftpSession.clientDebugInfo()+message1+((message2 ==null)?"":" "+message2));
		}
	}
	public static void warnSessionInfo(Logger logger,FTPSession ftpSession,String message1,String message2)
	{
		logger.warn(ftpSession.clientDebugInfo()+message1+((message2 ==null)?"":" "+message2));
	}
	public static void warnSessionInfo(Logger logger,FTPSession ftpSession,String message,Exception e)
	{
		logger.warn(ftpSession.clientDebugInfo()+message,e);
	}
	public static void logSessionInfo(Logger logger,FTPSession ftpSession,String message)
	{
		logger.info(ftpSession.clientDebugInfo()+message);
	}
	public void releaseNIOBuffer()
	{
		if(this.readBuffer!=null)
		{
			ByteBuffer buf=readBuffer.getBuffer();
			ftpSession.bufPool.recycleBuf(buf);
			readBuffer=null;
		}
	}
	
}
