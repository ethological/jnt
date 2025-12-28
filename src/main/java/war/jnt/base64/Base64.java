package war.jnt.base64;

/**
 * Zero dependency, pure java implementation of the Base64 encoding
 * @since 26 Aug 2025
 * @see java.util.Base64
 * @author Jan
 */
public final class Base64
{
    /**
     * Dictionary used to build a base64-encoded output
     */
    private static final char[] b64Dict = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    /**
     * Not much to explain here, I would guess
     */
    private static final char padding = '=';

    /**
     * Encodes bytes to base64
     * @param input Input bytes
     * @return Input bytes encoded in base64
     */
    public static byte[] encode(final byte[] input)
    {
        if (input == null || input.length == 0) // yeah you might not want to do that lmao
        {
            throw new IllegalArgumentException("Source null (or empty)");
        }

        final byte[] out = new byte[4 * ((input.length + 2) / 3)];

        int inputIdx = 0;
        int outputIdx = 0;

        while (inputIdx + 2 < input.length)
        {
            // i cba to clean this up ngl
            final int bits = ((input[inputIdx++] & 0xff) << 16) |
                    ((input[inputIdx++] & 0xff) << 8)  |
                    (input[inputIdx++] & 0xff);

            out[outputIdx++] = (byte) b64Dict[(bits >>> 18) & 0x3f];
            out[outputIdx++] = (byte) b64Dict[(bits >>> 12) & 0x3f];
            out[outputIdx++] = (byte) b64Dict[(bits >>> 6)  & 0x3f];
            out[outputIdx++] = (byte) b64Dict[bits & 0x3f];
        }

        final int leftOverBytes = input.length - inputIdx; // took a byte (bite) here :3

        if (leftOverBytes == 0) return out; // to prevent a AIOOBE below :sob:

        // basically not needed but imma do the padding for completeness sake
        final int high = (input[inputIdx] & 0xff) << 16;

        if (leftOverBytes == 1)
        {
            out[outputIdx++] = (byte) b64Dict[(high >>> 18) & 0x3f];
            out[outputIdx++] = (byte) b64Dict[(high >>> 12) & 0x3f];
            out[outputIdx++] = (byte) padding;
            out[outputIdx]   = (byte) padding;
        }
        else if (leftOverBytes == 2)
        {
            final int low = (input[inputIdx + 1] & 0xff) << 8;
            final int bits = high | low;

            out[outputIdx++] = (byte) b64Dict[(bits >>> 18) & 0x3f];
            out[outputIdx++] = (byte) b64Dict[(bits >>> 12) & 0x3f];
            out[outputIdx++] = (byte) b64Dict[(bits >>> 6)  & 0x3f];
            out[outputIdx]   = (byte) padding;
        }

        return out;
    }

    /**
     * Decodes bytes from base64
     * @param input Input bytes
     * @return Input bytes decoded from base64
     */
    @SuppressWarnings("ExplicitArrayFilling") // I dont wanna use Arrays.fill (import -> hookable)
    public static byte[] decode(final byte[] input)
    {
        if (input == null || input.length == 0)
        {
            throw new IllegalArgumentException("Source null (or empty)");
        }
        if (input.length % 4 != 0) // yeah fuck you atp
        {
            throw new IllegalArgumentException("Base64 formatting invalid");
        }

        // yeah I stole this shit from Base64$Decoder class
        final int[] fromBase64 = new int[256];
        for (int i = 0; i < fromBase64.length; i++) fromBase64[i] = -1;
        for (int i = 0; i < b64Dict.length; i++) fromBase64[b64Dict[i]] = i;

        int paddingCount = 0;
        if (input[input.length - 1] == padding) paddingCount++;
        if (input[input.length - 2] == padding) paddingCount++;

        final int outLen = (input.length / 4) * 3 - paddingCount;
        final byte[] out = new byte[outLen];

        int inputIdx = 0;
        int outputIdx = 0;

        while (inputIdx < input.length)
        {
            final int c1 = input[inputIdx++] & 0xff;
            final int c2 = input[inputIdx++] & 0xff;
            final int c3 = input[inputIdx++] & 0xff;
            final int c4 = input[inputIdx++] & 0xff;

            // fuckass code
            final int b1 = (c1 == padding) ? 0 : fromBase64[c1];
            final int b2 = (c2 == padding) ? 0 : fromBase64[c2];
            final int b3 = (c3 == padding) ? 0 : fromBase64[c3];
            final int b4 = (c4 == padding) ? 0 : fromBase64[c4];

            final int bits = (b1 << 18) | (b2 << 12) | (b3 << 6) | b4;

            if (outputIdx < outLen) out[outputIdx++] = (byte) ((bits >>> 16) & 0xff);
            if (outputIdx < outLen) out[outputIdx++] = (byte) ((bits >>> 8) & 0xff);
            if (outputIdx < outLen) out[outputIdx++] = (byte) (bits & 0xff);
        }

        return out;
    }
}