package nioftpproxy;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BufferPool {

	private ArrayList<ByteBuffer> allBuffers=new ArrayList<ByteBuffer>();
	private final int bufferSize;
	public BufferPool(int size)
	{
		bufferSize=size;
	}
	public ByteBuffer allocByteBuffer()
	{
		if(!allBuffers.isEmpty())
		{
			//System.out.println("xxxx reused  ");
			return allBuffers.remove(0);
		}else
		{
			//System.out.println("xxx created  ");
			return ByteBuffer.allocateDirect(bufferSize);
		}
	}
	public void recycleBuf(ByteBuffer buf) {
		//System.out.println("xxx recyced "+buf);
		buf.position(0);
		buf.limit(buf.capacity());
		this.allBuffers.add(buf);
		
	}
}
