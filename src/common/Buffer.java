import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Buffer {
    private ByteArrayOutputStream buffer;
    private ByteBuffer readBuffer;
    private int cursorPos = 0;

    /**
     * Constructs an empty Buffer.
     */
    public Buffer() {
        buffer = new ByteArrayOutputStream();
    }

    /**
     * Constructs a Buffer with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity
     */
    public Buffer(int initialCapacity) {
        buffer = new ByteArrayOutputStream(initialCapacity);
    }

    /**
     * Constructs a Buffer initialized with the specified byte array.
     *
     * @param byteArray the byte array to initialize the buffer with
     */
    public Buffer(byte[] byteArray) {
        buffer = new ByteArrayOutputStream();
        buffer.write(byteArray, 0, byteArray.length);
        readBuffer = ByteBuffer.wrap(buffer.toByteArray());
    }

    /**
     * Puts a byte value into the buffer.
     *
     * @param value the byte value to put
     */
    public void putByte(byte value) {
        buffer.write(value);
    }

    /**
     * Puts a short value into the buffer.
     *
     * @param value the short value to put
     */
    public void putShort(short value) {
        buffer.write((value >> 8) & 0xFF);
        buffer.write(value & 0xFF);
    }

    /**
     * Puts an int value into the buffer.
     *
     * @param value the int value to put
     */
    public void putInt(int value) {
        buffer.write((value >> 24) & 0xFF);
        buffer.write((value >> 16) & 0xFF);
        buffer.write((value >> 8) & 0xFF);
        buffer.write(value & 0xFF);
    }

    /**
     * Puts a long value into the buffer.
     *
     * @param value the long value to put
     */
    public void putLong(long value) {
        for (int i = 7; i >= 0; i--) {
            buffer.write((int) (value >> (i * 8)) & 0xFF);
        }
    }

    /**
     * Puts a string value into the buffer.
     *
     * @param value the string value to put
     */
    public void putStringRaw(String value) {
        byte[] strBytes = value.getBytes(StandardCharsets.US_ASCII);
        buffer.write(strBytes, 0, strBytes.length);
    }

    /**
     * Puts a string value into the buffer.
     * Precedes it with a len value (short)
     *
     * @param value the string value to put
     */
    public void putString(String value) {
        byte[] strBytes = value.getBytes(StandardCharsets.US_ASCII);
        putShort((short)strBytes.length);
        buffer.write(strBytes, 0, strBytes.length);
    }

    /**
     * Returns the buffer content as a byte array.
     *
     * @return the byte array
     */
    public byte[] toArray() {
        return buffer.toByteArray();
    }

    /**
     * Returns the buffer content as a byte array preceded by its length (excluding the length bytes).
     *
     * @return the byte array with length
     */
    public byte[] toArrayWithLen() {
        byte[] content = buffer.toByteArray();
        int length = content.length;
        ByteBuffer byteBuffer = ByteBuffer.allocate(2 + length);
        byteBuffer.putShort((short) length);
        byteBuffer.put(content);
        return byteBuffer.array();
    }

    /**
     * Returns the size of the buffer.
     *
     * @return the size of the buffer
     */
    public int size() {
        return buffer.size();
    }

    /**
     * Converts the buffer to a ByteBuffer.
     *
     * @return the ByteBuffer
     */
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(buffer.toByteArray());
    }

    /**
     * Puts an array of bytes into the buffer.
     *
     * @param src the byte array to put
     */
    public void put(byte[] src) {
        buffer.write(src, 0, src.length);
    }
    
    /**
     * Alias for put(byte[]) - Puts an array of bytes into the buffer.
     *
     * @param src the byte array to put
     */
    public void putBytes(byte[] src) {
        put(src);
    }

    /**
     * Puts a portion of an array of bytes into the buffer.
     *
     * @param src    the byte array to put
     * @param offset the starting offset in the byte array
     * @param length the number of bytes to put
     */
    public void put(byte[] src, int offset, int length) {
        buffer.write(src, offset, length);
    }

    /**
     * Gets a byte value from the buffer.
     *
     * @return the byte value
     */
    public byte getByte() {
        ensureReadBufferInitialized();
        byte value = readBuffer.get(cursorPos);
        cursorPos++;
        return value;
    }

    /**
     * Gets a short value from the buffer.
     *
     * @return the short value
     */
    public short getShort() {
        ensureReadBufferInitialized();
        short value = readBuffer.getShort(cursorPos);
        cursorPos += 2;
        return value;
    }

    /**
     * Gets an int value from the buffer.
     *
     * @return the int value
     */
    public int getInt() {
        ensureReadBufferInitialized();
        int value = readBuffer.getInt(cursorPos);
        cursorPos += 4;
        return value;
    }

    /**
     * Gets a long value from the buffer.
     *
     * @return the long value
     */
    public long getLong() {
        ensureReadBufferInitialized();
        long value = readBuffer.getLong(cursorPos);
        cursorPos += 8;
        return value;
    }

    /**
     * Gets a string value from the buffer.
     *
     * @return the string value
     */
    public String getString(int strlen) {
        ensureReadBufferInitialized();
        // int strLength = getShort();
        byte[] strBytes = new byte[strlen];
        readBuffer.position(cursorPos);
        readBuffer.get(strBytes, 0, strlen);
        cursorPos += strlen;
        return new String(strBytes, StandardCharsets.US_ASCII);
    }

    /**
     * Gets a string value from the buffer.
     * Assumes that it is preceeded by a length value (short)
     * |len|str|
     *
     * @return the string value
     */
    public String getString() {
        ensureReadBufferInitialized();
        int strLength = getShort();
        byte[] strBytes = new byte[strLength];
        readBuffer.position(cursorPos);
        readBuffer.get(strBytes, 0, strLength);
        cursorPos += strLength;
        return new String(strBytes, StandardCharsets.US_ASCII);
    }

    /**
     * Returns a new Buffer containing the remaining bytes from the current cursor position.
     *
     * @return the new Buffer
     */
    public Buffer slice() {
        ensureReadBufferInitialized();
        byte[] remainingBytes = new byte[readBuffer.remaining()];
        readBuffer.get(remainingBytes);
        return new Buffer(remainingBytes);
    }

    /**
     * Returns the number of remaining bytes between the current cursor position and the end of the buffer.
     *
     * @return the number of remaining bytes
     */
    public int remaining() {
        ensureReadBufferInitialized();
        return readBuffer.limit() - cursorPos;
    }

    /**
     * Ensures that the read buffer is initialized.
     */
    private void ensureReadBufferInitialized() {
        if (readBuffer == null) {
            readBuffer = ByteBuffer.wrap(buffer.toByteArray());
        }
    }
}
