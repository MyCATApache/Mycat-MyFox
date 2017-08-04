package nioftpproxy;

import java.io.IOException;

public class FTPUtil {

	
	
    public static int parsePort(String s) throws IOException {
        int port;
        try {
            int i = s.lastIndexOf('(');
            int j = s.lastIndexOf(')');
            if ((i != -1) && (j != -1) && (i < j)) {
                s = s.substring(i + 1, j);
            }
            
            i = s.lastIndexOf(',');
            j = s.lastIndexOf(',', i - 1);
            
            port = Integer.parseInt(s.substring(i + 1));
            port += 256 * Integer.parseInt(s.substring(j + 1, i));
        } catch (Exception e) {
            throw new IOException();
        }
        return port;
    }
}
