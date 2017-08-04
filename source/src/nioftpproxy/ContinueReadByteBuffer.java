package nioftpproxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 多次重复读取的ByteBuffer,前面部分的数据可能是处理过的，新的数据报文第一个字节的位置为startPos;
 * @author wuzhihui
 *
 */
public class ContinueReadByteBuffer {
private final ByteBuffer buffer;	
private int startPos;
public ContinueReadByteBuffer(ByteBuffer buffer) {
	super();
	this.buffer = buffer;
} 
public int read(SocketChannel channel) throws IOException
{
	int readed= channel.read(buffer);
	if(!buffer.hasRemaining())
	{
		buffer.position(startPos);
		buffer.compact();
		startPos=0;
		//read again
		readed+=channel.read(buffer);
	}
	return readed;
	
}
public String getString(int from,int end) 
{
	byte[] data=new byte[end-from];
	for(int i=0;i<data.length;i++)
	{
		data[i]=buffer.get(startPos+i);
	}
	try {
		return new String(data,"utf-8");
	} catch (UnsupportedEncodingException e) {
		throw new RuntimeException(e);
	}
}
public int findBytesTwo(byte[] byte2)
{
	int endPos=getEndPos();
	for(int i=this.startPos;i<endPos;i++)
	{
		if(buffer.get(i)==byte2[0] )
		{
			if(i+1<endPos)
			{
				if(buffer.get(i+1)==byte2[1])
				{
					return i-this.startPos;
				}else
				{
					i=i+1;
				}
			}else
			{
				return -1;
			}
		}
	}
	return -1;
}

public ByteBuffer getBuffer() {
	return buffer;
}
public int getEndPos()
{
	return buffer.position();
}
public byte getByte(int index)
{
	return buffer.get(index);
}
public int getStartPos() {
	return startPos;
}
public void setStartPos(int startPos) {
	this.startPos = startPos;
}

}
