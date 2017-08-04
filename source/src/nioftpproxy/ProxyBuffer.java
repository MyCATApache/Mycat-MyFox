package nioftpproxy;


import java.nio.ByteBuffer;
public class ProxyBuffer {

    public static enum BufferState {

        READY_TO_WRITE, READY_TO_READ

    }


    private final ByteBuffer buffer;
    
    private BufferState state = BufferState.READY_TO_WRITE;

    public ProxyBuffer(ByteBuffer buffer) {
		super();
		this.buffer = buffer;
	}

	public boolean isReadyToRead() {
        return state == BufferState.READY_TO_READ;
    }

    public boolean isReadyToWrite() {
        return state == BufferState.READY_TO_WRITE;
    }

	public BufferState getState() {
		return state;
	}

	public void setState(BufferState state) {
		this.state = state;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

   

}

