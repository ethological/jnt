package war.jnt.crypto;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * @author etho
 * @since 2025/03/11
 * AES-CBC 128-bit
 */
public class Crypto {
    public static byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) {
        validate(key, iv);
        byte[] padded = pad(plaintext);
        byte[] out = new byte[padded.length];
        byte[] previousBlock = Arrays.copyOf(iv, 16);

        for (int offset = 0; offset < padded.length; offset += 16) {
            byte[] block = bxor(padded, offset, previousBlock);
            encryptBlock(block, key);
            System.arraycopy(block, 0, out, offset, 16);
            previousBlock = block; // block chaining (CBC)
        }
        return out;
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) {
        if (ciphertext.length % 16 != 0) {
            throw new IllegalArgumentException("Ciphertext length must be multiple of 16 bytes");
        }
        validate(key, iv);
        byte[] out = new byte[ciphertext.length];
        byte[] previousBlock = Arrays.copyOf(iv, 16);

        for (int offset = 0; offset < ciphertext.length; offset += 16) {
            byte[] block = Arrays.copyOfRange(ciphertext, offset, offset + 16);
            byte[] working = Arrays.copyOf(block, 16);
            decryptBlock(working, key);
            byte[] plainBlock = bxor(working, 0, previousBlock);
            System.arraycopy(plainBlock, 0, out, offset, 16);
            previousBlock = block; // block chaining (CBC)
        }
        return unpad(out);
    }

    private static final int N_ROUNDS = 10;

    private static final SecureRandom RAND = new SecureRandom();

    private static final byte[] SBOX = new byte[] {
            (byte)0x63,(byte)0x7c,(byte)0x77,(byte)0x7b,(byte)0xf2,(byte)0x6b,(byte)0x6f,(byte)0xc5,
            (byte)0x30,(byte)0x01,(byte)0x67,(byte)0x2b,(byte)0xfe,(byte)0xd7,(byte)0xab,(byte)0x76,
            (byte)0xca,(byte)0x82,(byte)0xc9,(byte)0x7d,(byte)0xfa,(byte)0x59,(byte)0x47,(byte)0xf0,
            (byte)0xad,(byte)0xd4,(byte)0xa2,(byte)0xaf,(byte)0x9c,(byte)0xa4,(byte)0x72,(byte)0xc0,
            (byte)0xb7,(byte)0xfd,(byte)0x93,(byte)0x26,(byte)0x36,(byte)0x3f,(byte)0xf7,(byte)0xcc,
            (byte)0x34,(byte)0xa5,(byte)0xe5,(byte)0xf1,(byte)0x71,(byte)0xd8,(byte)0x31,(byte)0x15,
            (byte)0x04,(byte)0xc7,(byte)0x23,(byte)0xc3,(byte)0x18,(byte)0x96,(byte)0x05,(byte)0x9a,
            (byte)0x07,(byte)0x12,(byte)0x80,(byte)0xe2,(byte)0xeb,(byte)0x27,(byte)0xb2,(byte)0x75,
            (byte)0x09,(byte)0x83,(byte)0x2c,(byte)0x1a,(byte)0x1b,(byte)0x6e,(byte)0x5a,(byte)0xa0,
            (byte)0x52,(byte)0x3b,(byte)0xd6,(byte)0xb3,(byte)0x29,(byte)0xe3,(byte)0x2f,(byte)0x84,
            (byte)0x53,(byte)0xd1,(byte)0x00,(byte)0xed,(byte)0x20,(byte)0xfc,(byte)0xb1,(byte)0x5b,
            (byte)0x6a,(byte)0xcb,(byte)0xbe,(byte)0x39,(byte)0x4a,(byte)0x4c,(byte)0x58,(byte)0xcf,
            (byte)0xd0,(byte)0xef,(byte)0xaa,(byte)0xfb,(byte)0x43,(byte)0x4d,(byte)0x33,(byte)0x85,
            (byte)0x45,(byte)0xf9,(byte)0x02,(byte)0x7f,(byte)0x50,(byte)0x3c,(byte)0x9f,(byte)0xa8,
            (byte)0x51,(byte)0xa3,(byte)0x40,(byte)0x8f,(byte)0x92,(byte)0x9d,(byte)0x38,(byte)0xf5,
            (byte)0xbc,(byte)0xb6,(byte)0xda,(byte)0x21,(byte)0x10,(byte)0xff,(byte)0xf3,(byte)0xd2,
            (byte)0xcd,(byte)0x0c,(byte)0x13,(byte)0xec,(byte)0x5f,(byte)0x97,(byte)0x44,(byte)0x17,
            (byte)0xc4,(byte)0xa7,(byte)0x7e,(byte)0x3d,(byte)0x64,(byte)0x5d,(byte)0x19,(byte)0x73,
            (byte)0x60,(byte)0x81,(byte)0x4f,(byte)0xdc,(byte)0x22,(byte)0x2a,(byte)0x90,(byte)0x88,
            (byte)0x46,(byte)0xee,(byte)0xb8,(byte)0x14,(byte)0xde,(byte)0x5e,(byte)0x0b,(byte)0xdb,
            (byte)0xe0,(byte)0x32,(byte)0x3a,(byte)0x0a,(byte)0x49,(byte)0x06,(byte)0x24,(byte)0x5c,
            (byte)0xc2,(byte)0xd3,(byte)0xac,(byte)0x62,(byte)0x91,(byte)0x95,(byte)0xe4,(byte)0x79,
            (byte)0xe7,(byte)0xc8,(byte)0x37,(byte)0x6d,(byte)0x8d,(byte)0xd5,(byte)0x4e,(byte)0xa9,
            (byte)0x6c,(byte)0x56,(byte)0xf4,(byte)0xea,(byte)0x65,(byte)0x7a,(byte)0xae,(byte)0x08,
            (byte)0xba,(byte)0x78,(byte)0x25,(byte)0x2e,(byte)0x1c,(byte)0xa6,(byte)0xb4,(byte)0xc6,
            (byte)0xe8,(byte)0xdd,(byte)0x74,(byte)0x1f,(byte)0x4b,(byte)0xbd,(byte)0x8b,(byte)0x8a,
            (byte)0x70,(byte)0x3e,(byte)0xb5,(byte)0x66,(byte)0x48,(byte)0x03,(byte)0xf6,(byte)0x0e,
            (byte)0x61,(byte)0x35,(byte)0x57,(byte)0xb9,(byte)0x86,(byte)0xc1,(byte)0x1d,(byte)0x9e,
            (byte)0xe1,(byte)0xf8,(byte)0x98,(byte)0x11,(byte)0x69,(byte)0xd9,(byte)0x8e,(byte)0x94,
            (byte)0x9b,(byte)0x1e,(byte)0x87,(byte)0xe9,(byte)0xce,(byte)0x55,(byte)0x28,(byte)0xdf,
            (byte)0x8c,(byte)0xa1,(byte)0x89,(byte)0x0d,(byte)0xbf,(byte)0xe6,(byte)0x42,(byte)0x68,
            (byte)0x41,(byte)0x99,(byte)0x2d,(byte)0x0f,(byte)0xb0,(byte)0x54,(byte)0xbb,(byte)0x16
    };

    private static final byte[] INVERSE_SBOX = new byte[] {
            (byte)0x52,(byte)0x09,(byte)0x6a,(byte)0xd5,(byte)0x30,(byte)0x36,(byte)0xa5,(byte)0x38,
            (byte)0xbf,(byte)0x40,(byte)0xa3,(byte)0x9e,(byte)0x81,(byte)0xf3,(byte)0xd7,(byte)0xfb,
            (byte)0x7c,(byte)0xe3,(byte)0x39,(byte)0x82,(byte)0x9b,(byte)0x2f,(byte)0xff,(byte)0x87,
            (byte)0x34,(byte)0x8e,(byte)0x43,(byte)0x44,(byte)0xc4,(byte)0xde,(byte)0xe9,(byte)0xcb,
            (byte)0x54,(byte)0x7b,(byte)0x94,(byte)0x32,(byte)0xa6,(byte)0xc2,(byte)0x23,(byte)0x3d,
            (byte)0xee,(byte)0x4c,(byte)0x95,(byte)0x0b,(byte)0x42,(byte)0xfa,(byte)0xc3,(byte)0x4e,
            (byte)0x08,(byte)0x2e,(byte)0xa1,(byte)0x66,(byte)0x28,(byte)0xd9,(byte)0x24,(byte)0xb2,
            (byte)0x76,(byte)0x5b,(byte)0xa2,(byte)0x49,(byte)0x6d,(byte)0x8b,(byte)0xd1,(byte)0x25,
            (byte)0x72,(byte)0xf8,(byte)0xf6,(byte)0x64,(byte)0x86,(byte)0x68,(byte)0x98,(byte)0x16,
            (byte)0xd4,(byte)0xa4,(byte)0x5c,(byte)0xcc,(byte)0x5d,(byte)0x65,(byte)0xb6,(byte)0x92,
            (byte)0x6c,(byte)0x70,(byte)0x48,(byte)0x50,(byte)0xfd,(byte)0xed,(byte)0xb9,(byte)0xda,
            (byte)0x5e,(byte)0x15,(byte)0x46,(byte)0x57,(byte)0xa7,(byte)0x8d,(byte)0x9d,(byte)0x84,
            (byte)0x90,(byte)0xd8,(byte)0xab,(byte)0x00,(byte)0x8c,(byte)0xbc,(byte)0xd3,(byte)0x0a,
            (byte)0xf7,(byte)0xe4,(byte)0x58,(byte)0x05,(byte)0xb8,(byte)0xb3,(byte)0x45,(byte)0x06,
            (byte)0xd0,(byte)0x2c,(byte)0x1e,(byte)0x8f,(byte)0xca,(byte)0x3f,(byte)0x0f,(byte)0x02,
            (byte)0xc1,(byte)0xaf,(byte)0xbd,(byte)0x03,(byte)0x01,(byte)0x13,(byte)0x8a,(byte)0x6b,
            (byte)0x3a,(byte)0x91,(byte)0x11,(byte)0x41,(byte)0x4f,(byte)0x67,(byte)0xdc,(byte)0xea,
            (byte)0x97,(byte)0xf2,(byte)0xcf,(byte)0xce,(byte)0xf0,(byte)0xb4,(byte)0xe6,(byte)0x73,
            (byte)0x96,(byte)0xac,(byte)0x74,(byte)0x22,(byte)0xe7,(byte)0xad,(byte)0x35,(byte)0x85,
            (byte)0xe2,(byte)0xf9,(byte)0x37,(byte)0xe8,(byte)0x1c,(byte)0x75,(byte)0xdf,(byte)0x6e,
            (byte)0x47,(byte)0xf1,(byte)0x1a,(byte)0x71,(byte)0x1d,(byte)0x29,(byte)0xc5,(byte)0x89,
            (byte)0x6f,(byte)0xb7,(byte)0x62,(byte)0x0e,(byte)0xaa,(byte)0x18,(byte)0xbe,(byte)0x1b,
            (byte)0xfc,(byte)0x56,(byte)0x3e,(byte)0x4b,(byte)0xc6,(byte)0xd2,(byte)0x79,(byte)0x20,
            (byte)0x9a,(byte)0xdb,(byte)0xc0,(byte)0xfe,(byte)0x78,(byte)0xcd,(byte)0x5a,(byte)0xf4,
            (byte)0x1f,(byte)0xdd,(byte)0xa8,(byte)0x33,(byte)0x88,(byte)0x07,(byte)0xc7,(byte)0x31,
            (byte)0xb1,(byte)0x12,(byte)0x10,(byte)0x59,(byte)0x27,(byte)0x80,(byte)0xec,(byte)0x5f,
            (byte)0x60,(byte)0x51,(byte)0x7f,(byte)0xa9,(byte)0x19,(byte)0xb5,(byte)0x4a,(byte)0x0d,
            (byte)0x2d,(byte)0xe5,(byte)0x7a,(byte)0x9f,(byte)0x93,(byte)0xc9,(byte)0x9c,(byte)0xef,
            (byte)0xa0,(byte)0xe0,(byte)0x3b,(byte)0x4d,(byte)0xae,(byte)0x2a,(byte)0xf5,(byte)0xb0,
            (byte)0xc8,(byte)0xeb,(byte)0xbb,(byte)0x3c,(byte)0x83,(byte)0x53,(byte)0x99,(byte)0x61,
            (byte)0x17,(byte)0x2b,(byte)0x04,(byte)0x7e,(byte)0xba,(byte)0x77,(byte)0xd6,(byte)0x26,
            (byte)0xe1,(byte)0x69,(byte)0x14,(byte)0x63,(byte)0x55,(byte)0x21,(byte)0x0c,(byte)0x7d
    };

    private static final byte[] RCON = new byte[] {
            (byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80,(byte)0x1b,(byte)0x36
    };

    private static void encryptBlock(byte[] state, byte[] key) {
        byte[] roundKeys = keyExpansion(key);
        addRoundKey(state, roundKeys, 0);
        for (int round = 1; round < N_ROUNDS; round++) {
            subBytes(state);
            shiftRows(state);
            mix(state);
            addRoundKey(state, roundKeys, round);
        }
        subBytes(state);
        shiftRows(state);
        addRoundKey(state, roundKeys, N_ROUNDS);
    }

    private static void decryptBlock(byte[] state, byte[] key) {
        byte[] roundKeys = keyExpansion(key);
        addRoundKey(state, roundKeys, N_ROUNDS);
        for (int round = N_ROUNDS - 1; round > 0; round--) {
            invShiftRows(state);
            invSubBytes(state);
            addRoundKey(state, roundKeys, round);
            inverseMix(state);
        }
        invShiftRows(state);
        invSubBytes(state);
        addRoundKey(state, roundKeys, 0);
    }

    private static void subBytes(byte[] state) {
        for (int i = 0; i < 16; i++) {
            state[i] = SBOX[state[i] & 0xFF];
        }
    }

    private static void invSubBytes(byte[] state) {
        for (int i = 0; i < 16; i++) {
            state[i] = INVERSE_SBOX[state[i] & 0xFF];
        }
    }

    private static void shiftRows(byte[] state) {
        byte tmp;
        tmp = state[1];
        state[1] = state[5];
        state[5] = state[9];
        state[9] = state[13];
        state[13] = tmp;

        tmp = state[2];
        byte tmp2 = state[6];
        state[2] = state[10];
        state[6] = state[14];
        state[10] = tmp;
        state[14] = tmp2;

        tmp = state[3];
        state[3] = state[15];
        state[15] = state[11];
        state[11] = state[7];
        state[7] = tmp;
    }

    private static void invShiftRows(byte[] state) {
        byte tmp;
        tmp = state[13];
        state[13] = state[9];
        state[9] = state[5];
        state[5] = state[1];
        state[1] = tmp;

        tmp = state[2];
        byte tmp2 = state[6];
        state[2] = state[10];
        state[6] = state[14];
        state[10] = tmp;
        state[14] = tmp2;

        tmp = state[3];
        state[3] = state[7];
        state[7] = state[11];
        state[11] = state[15];
        state[15] = tmp;
    }

    private static void mix(byte[] state) {
        for (int c = 0; c < 4; c++) {
            int i = c * 4;
            byte a0 = state[i];
            byte a1 = state[i + 1];
            byte a2 = state[i + 2];
            byte a3 = state[i + 3];
            state[i]     = (byte) (gmul(a0, 0x02) ^ gmul(a1, 0x03) ^ a2 ^ a3);
            state[i + 1] = (byte) (a0 ^ gmul(a1, 0x02) ^ gmul(a2, 0x03) ^ a3);
            state[i + 2] = (byte) (a0 ^ a1 ^ gmul(a2, 0x02) ^ gmul(a3, 0x03));
            state[i + 3] = (byte) (gmul(a0, 0x03) ^ a1 ^ a2 ^ gmul(a3, 0x02));
        }
    }

    private static void inverseMix(byte[] state) {
        for (int c = 0; c < 4; c++) {
            int i = c * 4;
            byte a0 = state[i];
            byte a1 = state[i + 1];
            byte a2 = state[i + 2];
            byte a3 = state[i + 3];
            state[i]     = (byte) (gmul(a0, 0x0e) ^ gmul(a1, 0x0b) ^ gmul(a2, 0x0d) ^ gmul(a3, 0x09));
            state[i + 1] = (byte) (gmul(a0, 0x09) ^ gmul(a1, 0x0e) ^ gmul(a2, 0x0b) ^ gmul(a3, 0x0d));
            state[i + 2] = (byte) (gmul(a0, 0x0d) ^ gmul(a1, 0x09) ^ gmul(a2, 0x0e) ^ gmul(a3, 0x0b));
            state[i + 3] = (byte) (gmul(a0, 0x0b) ^ gmul(a1, 0x0d) ^ gmul(a2, 0x09) ^ gmul(a3, 0x0e));
        }
    }

    private static void addRoundKey(byte[] state, byte[] roundKeys, int round) {
        int offset = round * 16;
        for (int i = 0; i < 16; i++) {
            state[i] ^= roundKeys[offset + i];
        }
    }

    private static byte[] keyExpansion(byte[] key) {
        if (key.length != 16) {
            throw new IllegalArgumentException("Key must be 16 bytes for AES-128");
        }

        byte[] w = new byte[16 * (N_ROUNDS + 1)];
        System.arraycopy(key, 0, w, 0, 16);

        int bytesGenerated = 16;
        int rconIter = 0;
        byte[] temp = new byte[4];

        while (bytesGenerated < w.length) {
            System.arraycopy(w, bytesGenerated - 4, temp, 0, 4);
            if (bytesGenerated % 16 == 0) {

                byte t = temp[0];
                temp[0] = temp[1];
                temp[1] = temp[2];
                temp[2] = temp[3];
                temp[3] = t;

                for (int i = 0; i < 4; i++) {
                    temp[i] = SBOX[temp[i] & 0xFF];
                }

                temp[0] ^= RCON[rconIter++];
            }
            for (int i = 0; i < 4; i++) {
                w[bytesGenerated] = (byte) (w[bytesGenerated - 16] ^ temp[i]);
                bytesGenerated++;
            }
        }
        return w;
    }

    private static byte gmul(byte a, int b) {
        int aa = a & 0xFF;
        int bb = b & 0xFF;
        int p = 0;
        for (int counter = 0; counter < 8; counter++) {
            if ((bb & 1) != 0) {
                p ^= aa;
            }
            boolean hiBitSet = (aa & 0x80) != 0;
            aa = (aa << 1) & 0xFF;
            if (hiBitSet) {
                aa ^= 0x1B;
            }
            bb >>= 1;
        }
        return (byte) p;
    }

    private static void validate(byte[] key, byte[] iv) {
        if (key.length != 16) {
            throw new IllegalArgumentException("Key must be 16 bytes (128-bit)");
        }
        if (iv.length != 16) {
            throw new IllegalArgumentException("IV must be 16 bytes");
        }
    }

    private static byte[] bxor(byte[] src, int offset, byte[] block) {
        byte[] out = new byte[16];
        for (int i = 0; i < 16; i++) {
            out[i] = (byte) (src[offset + i] ^ block[i]);
        }
        return out;
    }

    private static byte[] pad(byte[] input) {
        int padLen = 16 - (input.length % 16);
        byte[] out = Arrays.copyOf(input, input.length + padLen);
        Arrays.fill(out, input.length, out.length, (byte) padLen);
        return out;
    }

    private static byte[] unpad(byte[] input) {
        int padLen = input[input.length - 1] & 0xFF;
        if (padLen < 1 || padLen > 16) {
            throw new IllegalArgumentException("Invalid padding");
        }
        for (int i = 1; i <= padLen; i++) {
            if (input[input.length - i] != (byte) padLen) {
                throw new IllegalArgumentException("Invalid padding");
            }
        }
        return Arrays.copyOf(input, input.length - padLen);
    }
}
