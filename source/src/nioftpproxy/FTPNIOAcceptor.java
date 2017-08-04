package nioftpproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ecip.NewFtpProxy;

public class FTPNIOAcceptor extends Thread{
	 private final static Logger logger = Logger.getLogger(FTPNIOAcceptor.class);
	 
	public void run()
	{
		int nioIndex=0;
		Selector selector=null; 
		try {
			selector = Selector.open();
			final ServerSocketChannel serverChannel = ServerSocketChannel.open();
	        String bindAddr=NewFtpProxy.PROXY_CONFIG.bindAddress;
	        final InetSocketAddress isa = new InetSocketAddress(bindAddr,NewFtpProxy.PROXY_CONFIG.bindPort);
	        serverChannel.bind(isa);
	        serverChannel.configureBlocking(false);
	        String publicIP=!bindAddr.equals("0.0.0.0")?bindAddr:"127.0.0.1";
	        NewFtpProxy.PROXY_CONFIG.publicIP = publicIP.replace('.', ',');
	        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
   
		} catch (IOException e) {
			System.out.println(" FTPNIOAcceptor start err "+e);
			return;
		}
		System.out.println("*** Started FTP Proxy server *** ,NIO Threads "+NewFtpProxy.nioThreads.length+" listen on "+NewFtpProxy.PROXY_CONFIG.bindAddress+ ":"+NewFtpProxy.PROXY_CONFIG.bindPort+", pubic ip "+NewFtpProxy.PROXY_CONFIG.publicIP);
	    while (true) {
       	 try {
           int count=selector.select(1000);
           if(count==0)
           {
        	   continue;
           }
           final Set<SelectionKey> keys = selector.selectedKeys();
           for (final SelectionKey key : keys) {
               if (!key.isValid()) {
                   continue;
               }
               if (key.isAcceptable()) {
            	   ServerSocketChannel serverSocket=(ServerSocketChannel) key.channel();
                   final SocketChannel socketChannel = serverSocket.accept();
                   socketChannel.configureBlocking(false);
                   logger.info("new ftp client connected: "+socketChannel);
                   if(NewFtpProxy.PROXY_CONFIG.publicIP.equals("127,0,0,1"))
                   {
                   	
                    String	publicIP=((InetSocketAddress)socketChannel.getLocalAddress()).getHostString();
                   	logger.info("got ftp proxy server's public IP addr "+publicIP);
                   	NewFtpProxy.PROXY_CONFIG.publicIP=publicIP.replace(".", ",");
                   }
            	 //找到一个可用的NIO Reactor Thread，交付托管
       	        if(nioIndex++==Integer.MAX_VALUE)
       	        {
       	        	nioIndex=1;
       	        }
       	        int index=nioIndex%NewFtpProxy.nioThreads.length;
       	        NIOReactorThread nioReactor= NewFtpProxy.nioThreads[index];
       	        nioReactor.acceptNewSocketChannel(socketChannel);
               }
               else
               {
            	   logger.warn("not accept event "+key);
               }
           }
           keys.clear();
       }
    catch (IOException e) {
   	 logger.warn("caugh error ",e);
   }}
        
	}

}
