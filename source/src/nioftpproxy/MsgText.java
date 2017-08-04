package nioftpproxy;
/***
 * Text of server messages to client
 * */
public class MsgText {
	
	public static String msgConnect= "220 Java FTP Proxy Server (usage: USERID=user@site) ready."; 
	public static String msgConnectionRefused= "421 Connection refused, closing connection.";   
	public static String msgIncorrectSyntax= "531 Incorrect usage - closing connection.";
	public static String msgInternalError= "421 Internal error, closing connection.";
	public static String msgNotLoggedIn= "421 Internal error, closing connection.";
	public static String msgCannotAllocateLocalPort = "425 Cannot allocate local port...";
	public static String msgPortSuccess = "200 PORT command successful.";
	public static String msgPortFailed = "425 PORT command failed. Try PASV instead.";
	
}
