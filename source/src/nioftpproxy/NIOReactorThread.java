package nioftpproxy;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.ecip.NewFtpProxy;

/**
 * Thread implementation for listening local port. For each new socket-accept operation {@link ProxyTaskHandler}
 * instance created and attached to client and remote server channels.
 */
public class NIOReactorThread extends Thread {
    private final static long SELECTOR_TIMEOUT = 100;

    private final static Logger logger = Logger.getLogger(NIOReactorThread.class);
    private final Selector selector;
    private final BufferPool bufPool=new BufferPool(1024*10);
    private ConcurrentLinkedQueue<SocketChannel> pendingCons=new ConcurrentLinkedQueue<SocketChannel>();
    
    
    public NIOReactorThread() throws IOException {
        this.selector = Selector.open();
    }
  public void acceptNewSocketChannel(final SocketChannel socketChannel) throws IOException
  {
	  pendingCons.offer(socketChannel);
	   
  }
  
  private void processNewCons()
  {
	  SocketChannel socketChannel=null;
	  while((socketChannel=pendingCons.poll())!=null)
	  {
		  try
		  {
		  SelectionKey socketKey=socketChannel.register(selector, SelectionKey.OP_READ, null);
		  FTPSession ftpSession=new FTPSession();
			ftpSession.bufPool=bufPool;
			ftpSession.publicIP=NewFtpProxy.PROXY_CONFIG.publicIP;
			ftpSession.nioSelector=selector;
			ftpSession.clientCMDSocket=socketChannel;		
	       FTPCommandNIOHandler handler=new FTPCommandNIOHandler(socketKey,ftpSession);
	       socketKey.attach(handler);
		  }catch(Exception e)
		  {
			  logger.warn("regist new connection err "+e); 
		  }  
	  }
	 
  }
//    public void acceptNewSocketChannel(final SocketChannel socketChannel) throws IOException
//    {
//    	 SelectionKey socketKey=socketChannel.register(selector, SelectionKey.OP_READ, null);
//         FTPCommandNIOHandler handler=new FTPCommandNIOHandler(socketKey,socketChannel,publicIP,selector, socketChannel,bufPool);
//         socketKey.attach(handler);
//         selector.wakeup();
//    }
    public void run() {
    	long ioTimes=0;
            while (true) {
            	
            	 try {
                int selected=selector.select(SELECTOR_TIMEOUT);
                if(selected==0)
                {if(!pendingCons.isEmpty())
                    {
                	ioTimes=0;	
                	this.processNewCons();
                    }
                	continue;
                }else if((ioTimes>5)&!pendingCons.isEmpty())
                {
                	ioTimes=0;
                	this.processNewCons();
                }
                ioTimes++;
                final Set<SelectionKey> keys = selector.selectedKeys();
                for (final SelectionKey key : keys) {
                    if (!key.isValid()) {
                        continue;
                    }
                  if(key.isConnectable()) {
                        final NIOHandler handler = (NIOHandler) key.attachment();
                        handler.onConnected(key);
                    }
                  else if(key.isAcceptable()) {
                      final NIOServerHandler handler = (NIOServerHandler) key.attachment();
                      handler.onNewClient(key);
                  }
                    else {
                        final NIOHandler handler = (NIOHandler) key.attachment();
                        handler.handIO(key);
                    }
                }
                keys.clear();
            }
         catch (IOException e) {
        	 logger.warn("caugh error ",e);
        }}
    }

     public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
        }
    }
}
