import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class NetHelper {
    public static Socket createSocket(String ip, int port) throws IOException {
        Socket socket;

        socket = new Socket(InetAddress.getByName(ip), port);
        socket.setSoTimeout(30000);
        socket.setTcpNoDelay(true);
        return socket;
    }
    
    /**
     * Hash a username to a long value.
     * This is used for friend lists, ignore lists, and private messages.
     */
    public static long hashUsername(String username) {
        username = username.toLowerCase().trim();
        long hash = 0L;
        
        for (int i = 0; i < username.length() && i < 12; i++) {
            char c = username.charAt(i);
            hash *= 37L;
            
            if (c >= 'a' && c <= 'z') {
                hash += c - 'a' + 1;
            } else if (c >= '0' && c <= '9') {
                hash += c - '0' + 27;
            }
        }
        
        return hash;
    }
    
    /**
     * Set bits in a byte array at a specific bit offset.
     * Used for bit-packed packet data. This is the inverse of Utility.getBitMask().
     * 
     * @param buffer the byte array to write to
     * @param bitOffset the bit offset to start writing at
     * @param numBits the number of bits to write
     * @param value the value to write
     */
    public static void setBitMask(byte[] buffer, int bitOffset, int numBits, int value) {
        int bytePos = bitOffset >> 3; // bitOffset / 8
        int bitPos = 8 - (bitOffset & 7); // 8 - (bitOffset % 8)
        
        // Mask table for bit operations (same as client's bitmask array)
        int[] bitmask = {
            0, 1, 3, 7, 15, 31, 63, 127, 255, 511,
            1023, 2047, 4095, 8191, 16383, 32767, 65535, 0x1ffff, 0x3ffff, 0x7ffff,
            0xfffff, 0x1fffff, 0x3fffff, 0x7fffff, 0xffffff, 0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff, 0x1fffffff,
            0x3fffffff, 0x7fffffff, -1
        };
        
        // Write bits
        while (numBits > bitPos) {
            buffer[bytePos] &= ~bitmask[bitPos]; // Clear bits
            buffer[bytePos] |= (value >> (numBits - bitPos)) & bitmask[bitPos]; // Set bits
            numBits -= bitPos;
            bytePos++;
            bitPos = 8;
        }
        
        if (numBits == bitPos) {
            buffer[bytePos] &= ~bitmask[bitPos]; // Clear bits
            buffer[bytePos] |= value & bitmask[bitPos]; // Set bits
        } else {
            buffer[bytePos] &= ~(bitmask[numBits] << (bitPos - numBits)); // Clear bits
            buffer[bytePos] |= (value & bitmask[numBits]) << (bitPos - numBits); // Set bits
        }
    }
}
