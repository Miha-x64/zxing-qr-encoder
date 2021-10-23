package com.google.zxing.common;

final class BitArrayUtils {

    // Mike-MOVED from main/BitArray

    static void set(BitArray ba, int i) {
        ba.getBitArray()[i / 32] |= 1 << (i & 0x1F);
    }

    public static int getNextSet(BitArray ba, int from) {
        int size = ba.getSize();
        if (from >= size) {
            return size;
        }

        int[] bits = ba.getBitArray();
        int bitsOffset = from / 32;
        int currentBits = bits[bitsOffset];
        // mask off lesser bits first
        currentBits &= -(1 << (from & 0x1F));
        while (currentBits == 0) {
            if (++bitsOffset == bits.length) {
                return size;
            }
            currentBits = bits[bitsOffset];
        }
        int result = (bitsOffset * 32) + Integer.numberOfTrailingZeros(currentBits);
        return Math.min(result, size);
    }

    public static void setBulk(BitArray ba, int i, int newBits) {
        ba.getBitArray()[i / 32] = newBits;
    }

    public static void setRange(BitArray ba, int start, int end) {
        if (end < start || start < 0 || end > ba.getSize()) {
            throw new IllegalArgumentException();
        }
        if (end == start) {
            return;
        }
        end--; // will be easier to treat this as the last actually set bit -- inclusive
        int firstInt = start / 32;
        int lastInt = end / 32;
        int[] bits = ba.getBitArray();
        for (int i = firstInt; i <= lastInt; i++) {
            int firstBit = i > firstInt ? 0 : start & 0x1F;
            int lastBit = i < lastInt ? 31 : end & 0x1F;
            // Ones from firstBit to lastBit, inclusive
            int mask = (2 << lastBit) - (1 << firstBit);
            bits[i] |= mask;
        }
    }

    public static void clear(BitArray ba) {
        int[] bits = ba.getBitArray();
        int max = bits.length;
        for (int i = 0; i < max; i++) {
            bits[i] = 0;
        }
    }

    public static boolean isRange(BitArray ba, int start, int end, boolean value) {
        if (end < start || start < 0 || end > ba.getSize()) {
            throw new IllegalArgumentException();
        }
        if (end == start) {
            return true; // empty range matches
        }
        end--; // will be easier to treat this as the last actually set bit -- inclusive
        int firstInt = start / 32;
        int lastInt = end / 32;
        int[] bits = ba.getBitArray();
        for (int i = firstInt; i <= lastInt; i++) {
            int firstBit = i > firstInt ? 0 : start & 0x1F;
            int lastBit = i < lastInt ? 31 : end & 0x1F;
            // Ones from firstBit to lastBit, inclusive
            int mask = (2 << lastBit) - (1 << firstBit);

            // Return false if we're looking for 1s and the masked bits[i] isn't all 1s (that is,
            // equals the mask, or we're looking for 0s and the masked portion is not all 0s
            if ((bits[i] & mask) != (value ? mask : 0)) {
                return false;
            }
        }
        return true;
    }

}
