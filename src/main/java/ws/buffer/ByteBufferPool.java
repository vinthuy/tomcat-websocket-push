package ws.buffer;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * A pool of byte buffers that can be shared by multiple concurrent
 * clients.
 */
public final class ByteBufferPool {

    /**
     * The shared collection of byte buffers.
     */
    private final Queue<ByteBuffer> byteBuffers = new ConcurrentLinkedQueue<ByteBuffer>();

    /**
     * The size of each byte buffer.
     */
    private final int bufferSize;


    /**
     * Creates a new pool.
     */
    private ByteBufferPool(int bufferSize) {
        this.bufferSize = bufferSize;
    }


    /**
     * Creates a new pool.
     */
    public static ByteBufferPool newInstance(int bufferSize) {
        return new ByteBufferPool(bufferSize);
    }

    /**
     * Creates a new allocator associated with this pool.
     * The allocator will allow its client to allocate and release
     * buffers and will ensure that there are no more than
     * {@code maxBufferCount} buffers allocated through this allocator
     * at any given time moment.
     */
    public ByteBufferAllocator newAllocator(int maxBufferCount) {
        return new ByteBufferAllocatorImpl(maxBufferCount);
    }

    /**
     * The allocator implementation.
     */
    private final class ByteBufferAllocatorImpl implements ByteBufferAllocator {

        /**
         * The semaphore used to limit the number of buffers
         * allocated through this allocator.
         */
        private final Semaphore semaphore;


        /**
         * Creates a new allocator.
         */
        private ByteBufferAllocatorImpl(int maxBufferCount) {
            semaphore = new Semaphore(maxBufferCount);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public ByteBuffer allocate() throws InterruptedException {
            semaphore.acquire();
            ByteBuffer byteBuffer = byteBuffers.poll();
            if (byteBuffer == null) {
                byteBuffer = ByteBuffer.allocate(bufferSize);
            }
            if (!byteBuffer.hasArray()) {
                byteBuffer = ByteBuffer.allocate(bufferSize);
            }
            return byteBuffer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void release(ByteBuffer byteBuffer) {
            byteBuffers.add(byteBuffer);
            semaphore.release();
        }
    }
}
