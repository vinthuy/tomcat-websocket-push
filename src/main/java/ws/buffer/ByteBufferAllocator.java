package ws.buffer;

import java.nio.ByteBuffer;

/**
 * An object that can allocate and release byte buffers.
 */
public interface ByteBufferAllocator {

    /**
     * Allocates a byte buffer.
     */
    ByteBuffer allocate() throws InterruptedException;

    /**
     * Releases a byte buffer.
     */
    void release(ByteBuffer byteBuffer);
}