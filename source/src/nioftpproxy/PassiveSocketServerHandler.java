package nioftpproxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

public class PassiveSocketServerHandler implements NIOServerHandler{
	protected static Logger logger = Logger.getLogger(PassiveSocketServerHandler.class);
private FTPSession ftpSession;
	public PassiveSocketServerHandler(FTPSession ftpSession) throws IOException {
		this.ftpSession=ftpSession;
	}
	public void onNewClient(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocket=(ServerSocketChannel) key.channel();
        final SocketChannel socketChannel = serverSocket.accept();
        socketChannel.configureBlocking(false);
        ftpSession.clientDataSocket=socketChannel;
        socketChannel.register(ftpSession.nioSelector, SelectionKey.OP_READ, ftpSession.proxyTransDataHandler);
        AbstractFTPCmdHandler.logSessionInfo(logger, ftpSession, "successful connected to FTP Server data channel for data trans");
	}

}
