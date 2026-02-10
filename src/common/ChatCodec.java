package common;

/**
 * Chat message scramble/descramble codec shared between client and server.
 * This is the RSC chat compression algorithm.
 */
public class ChatCodec {

    private static final char[] CHARMAP = {
            ' ', 'e', 't', 'a', 'o', 'i', 'h', 'n', 's', 'r',
            'd', 'l', 'u', 'm', 'w', 'c', 'y', 'f', 'g', 'p',
            'b', 'v', 'k', 'x', 'j', 'q', 'z', '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', ' ', '!', '?',
            '.', ',', ':', ';', '(', ')', '-', '&', '*', '\\',
            '\'', '@', '#', '+', '=', '\243', '$', '%', '"', '[',
            ']'
    };

    /**
     * Scramble a chat message into compressed bytes.
     * @param message The message to scramble
     * @return The scrambled byte array
     */
    public static byte[] scramble(String message) {
        if (message.length() > 80)
            message = message.substring(0, 80);
        message = message.toLowerCase();

        byte[] result = new byte[100];
        int off = 0;
        int lshift = -1;

        for (int k = 0; k < message.length(); k++) {
            char currentChar = message.charAt(k);
            int foundIdx = 0;
            for (int n = 0; n < CHARMAP.length; n++) {
                if (currentChar == CHARMAP[n]) {
                    foundIdx = n;
                    break;
                }
            }

            if (foundIdx > 12)
                foundIdx += 195;
            if (lshift == -1) {
                if (foundIdx < 13)
                    lshift = foundIdx;
                else
                    result[off++] = (byte) foundIdx;
            } else if (foundIdx < 13) {
                result[off++] = (byte) ((lshift << 4) + foundIdx);
                lshift = -1;
            } else {
                result[off++] = (byte) ((lshift << 4) + (foundIdx >> 4));
                lshift = foundIdx & 0xf;
            }
        }

        if (lshift != -1)
            result[off++] = (byte) (lshift << 4);

        byte[] trimmed = new byte[off];
        System.arraycopy(result, 0, trimmed, 0, off);
        return trimmed;
    }

    /**
     * Descramble compressed bytes back into a chat message string.
     * @param data The scrambled data
     * @param offset Starting offset in the data array
     * @param length Number of bytes to descramble
     * @return The descrambled message
     */
    public static String descramble(byte[] data, int offset, int length) {
        try {
            char[] chars = new char[100];
            int newLen = 0;
            int l = -1;

            for (int idx = 0; idx < length; idx++) {
                int current = data[offset++] & 0xff;
                int k1 = current >> 4 & 0xf;
                if (l == -1) {
                    if (k1 < 13)
                        chars[newLen++] = CHARMAP[k1];
                    else
                        l = k1;
                } else {
                    chars[newLen++] = CHARMAP[((l << 4) + k1) - 195];
                    l = -1;
                }
                k1 = current & 0xf;
                if (l == -1) {
                    if (k1 < 13)
                        chars[newLen++] = CHARMAP[k1];
                    else
                        l = k1;
                } else {
                    chars[newLen++] = CHARMAP[((l << 4) + k1) - 195];
                    l = -1;
                }
            }

            boolean flag = true;
            for (int i = 0; i < newLen; i++) {
                char c = chars[i];
                if (i > 4 && c == '@')
                    chars[i] = ' ';
                if (c == '%')
                    chars[i] = ' ';
                if (flag && c >= 'a' && c <= 'z') {
                    chars[i] += '\uFFE0';
                    flag = false;
                }
                if (c == '.' || c == '!')
                    flag = true;
            }

            return new String(chars, 0, newLen);
        } catch (Exception e) {
            return ".";
        }
    }
}
