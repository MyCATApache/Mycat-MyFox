package nioftpproxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface NIOHandler {

	public static String CRLF = "\r\n";
	void onConnected(SelectionKey key) throws IOException;
	void handIO(SelectionKey key) throws IOException;
	void releaseNIOBuffer();
}
