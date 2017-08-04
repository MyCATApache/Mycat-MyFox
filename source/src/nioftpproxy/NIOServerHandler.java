package nioftpproxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface NIOServerHandler {
	void onNewClient(SelectionKey key) throws IOException;
}
