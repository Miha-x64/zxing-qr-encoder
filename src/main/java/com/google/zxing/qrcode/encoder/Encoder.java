/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.qrcode.encoder;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.GenericGFPoly;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class Encoder {

  // Mike-MOVED from StringUtils
  static final Charset SHIFT_JIS_CHARSET = Charset.forName("SJIS");

  // The original table is defined in the table 5 of JISX0510:2004 (p.19).
  private static final byte[] ALPHANUMERIC_TABLE = { // Mike-CHANGED from int[]
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,  // 0x50-0x5f
  };

  static final Charset DEFAULT_BYTE_MODE_ENCODING = StandardCharsets.ISO_8859_1;

  // Mike-MOVED from GenericGF
  public static final GenericGF QR_CODE_FIELD_256 = new GenericGF(0x011D, 256, 0); // x^8 + x^4 + x^3 + x^2 + 1

  // Mike-MOVED from MatrixUtil
  // From Appendix E. Table 1, JIS0510X:2004 (p 71). The table was double-checked by komatsu.
  static final byte[] POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE = {
      -1, -1, -1, -1,  -1,  -1,  -1,  // Version 1
       6, 18, -1, -1,  -1,  -1,  -1,  // Version 2
       6, 22, -1, -1,  -1,  -1,  -1,  // Version 3
       6, 26, -1, -1,  -1,  -1,  -1,  // Version 4
       6, 30, -1, -1,  -1,  -1,  -1,  // Version 5
       6, 34, -1, -1,  -1,  -1,  -1,  // Version 6
       6, 22, 38, -1,  -1,  -1,  -1,  // Version 7
       6, 24, 42, -1,  -1,  -1,  -1,  // Version 8
       6, 26, 46, -1,  -1,  -1,  -1,  // Version 9
       6, 28, 50, -1,  -1,  -1,  -1,  // Version 10
       6, 30, 54, -1,  -1,  -1,  -1,  // Version 11
       6, 32, 58, -1,  -1,  -1,  -1,  // Version 12
       6, 34, 62, -1,  -1,  -1,  -1,  // Version 13
       6, 26, 46, 66,  -1,  -1,  -1,  // Version 14
       6, 26, 48, 70,  -1,  -1,  -1,  // Version 15
       6, 26, 50, 74,  -1,  -1,  -1,  // Version 16
       6, 30, 54, 78,  -1,  -1,  -1,  // Version 17
       6, 30, 56, 82,  -1,  -1,  -1,  // Version 18
       6, 30, 58, 86,  -1,  -1,  -1,  // Version 19
       6, 34, 62, 90,  -1,  -1,  -1,  // Version 20
       6, 28, 50, 72,  94,  -1,  -1,  // Version 21
       6, 26, 50, 74,  98,  -1,  -1,  // Version 22
       6, 30, 54, 78, 102,  -1,  -1,  // Version 23
       6, 28, 54, 80, 106,  -1,  -1,  // Version 24
       6, 32, 58, 84, 110,  -1,  -1,  // Version 25
       6, 30, 58, 86, 114,  -1,  -1,  // Version 26
       6, 34, 62, 90, 118,  -1,  -1,  // Version 27
       6, 26, 50, 74,  98, 122,  -1,  // Version 28
       6, 30, 54, 78, 102, 126,  -1,  // Version 29
       6, 26, 52, 78, 104, (byte) 130,  -1,  // Version 30
       6, 30, 56, 82, 108, (byte) 134,  -1,  // Version 31
       6, 34, 60, 86, 112, (byte) 138,  -1,  // Version 32
       6, 30, 58, 86, 114, (byte) 142,  -1,  // Version 33
       6, 34, 62, 90, 118, (byte) 146,  -1,  // Version 34
       6, 30, 54, 78, 102, 126, (byte) 150,  // Version 35
       6, 24, 50, 76, 102, (byte) 128, (byte) 154,  // Version 36
       6, 28, 54, 80, 106, (byte) 132, (byte) 158,  // Version 37
       6, 32, 58, 84, 110, (byte) 136, (byte) 162,  // Version 38
       6, 26, 54, 82, 110, (byte) 138, (byte) 166,  // Version 39
       6, 30, 58, 86, 114, (byte) 142, (byte) 170,  // Version 40
  };

  private Encoder() {
  }

  // The mask penalty calculation is complicated.  See Table 21 of JISX0510:2004 (p.45) for details.
  // Basically it applies four rules and summate all penalties.
  private static int calculateMaskPenalty(ByteMatrix matrix) {
    // Mike-CHANGED: inlined applyMaskPenaltyRule1
    return MaskUtil.applyMaskPenaltyRule1Internal(matrix, true) +
        MaskUtil.applyMaskPenaltyRule1Internal(matrix, false) +
        MaskUtil.applyMaskPenaltyRule2(matrix) +
        MaskUtil.applyMaskPenaltyRule3(matrix) +
        MaskUtil.applyMaskPenaltyRule4(matrix);
  }

  /**
   * @param content text to encode
   * @param ecLevel error correction level to use
   * @return {@link QRCode} representing the encoded QR code
   * @throws WriterException if encoding can't succeed, because of for example invalid content
   *   or configuration
   */
  public static QRCode encode(String content, ErrorCorrectionLevel ecLevel) throws WriterException {
    return encode(content, ecLevel, null);
  }

  public static QRCode encode(String content,
                              ErrorCorrectionLevel ecLevel,
                              Map<EncodeHintType,?> hints) throws WriterException {

    int version;
    BitArray headerAndDataBits;
    Mode mode;

    boolean hasGS1FormatHint = hints != null && hints.containsKey(EncodeHintType.GS1_FORMAT) &&
        Boolean.parseBoolean(hints.get(EncodeHintType.GS1_FORMAT).toString());
    boolean hasCompactionHint = hints != null && hints.containsKey(EncodeHintType.QR_COMPACT) &&
        Boolean.parseBoolean(hints.get(EncodeHintType.QR_COMPACT).toString());

    // Determine what character encoding has been specified by the caller, if any
    Charset encoding = DEFAULT_BYTE_MODE_ENCODING;
    boolean hasEncodingHint = hints != null && hints.containsKey(EncodeHintType.CHARACTER_SET);
    if (hasEncodingHint) {
      encoding = Charset.forName(hints.get(EncodeHintType.CHARACTER_SET).toString());
    }

    if (hasCompactionHint) {
      mode = Mode.BYTE;

      Charset priorityEncoding = encoding.equals(DEFAULT_BYTE_MODE_ENCODING) ? null : encoding;
      // Mike-CHANGED: getting version to int[], inlined ResultList class
      int[] tmpVersion = new int[1];
      List<MinimalEncoder.ResultNode> rn = // Mike-CHANGED to static call
          MinimalEncoder.encode(content, priorityEncoding, hasGS1FormatHint, ecLevel, tmpVersion);

      headerAndDataBits = new BitArray();
      version = tmpVersion[0];

      // Mike-CHANGED inlined MinimalEncoder.getBits
      for (MinimalEncoder.ResultNode resultNode : rn) { // Mike-CHANGED parameters
        resultNode.getBits(headerAndDataBits, version);
      }

    } else {

      // Pick an encoding mode appropriate for the content. Note that this will not attempt to use
      // multiple modes / segments even if that were more efficient.
      mode = chooseMode(content, encoding);

      // This will store the header information, like mode and
      // length, as well as "header" segments like an ECI segment.
      BitArray headerBits = new BitArray();

      // Append ECI segment if applicable
      if (mode == Mode.BYTE && hasEncodingHint) {
        Integer eci = eciByName(encoding.name());
        if (eci != null) {
          appendECI(eci, headerBits);
        }
      }

      // Append the FNC1 mode header for GS1 formatted data if applicable
      if (hasGS1FormatHint) {
        // GS1 formatted codes are prefixed with a FNC1 in first position mode header
        headerBits.appendBits(Mode.FNC1_FIRST_POSITION.getBits(), 4); // Mike-CHANGED: inlined appendModeInfo
      }

      // (With ECI in place,) Write the mode marker
      headerBits.appendBits(mode.getBits(), 4); // Mike-CHANGED: inlined appendModeInfo

      // Collect data within the main segment, separately, to count its size if needed. Don't add it to
      // main payload yet.
      BitArray dataBits = new BitArray();
      // Mike-CHANGED: passing range
      appendBytes(content, 0, content.length(), mode, dataBits, encoding);

      if (hints != null && hints.containsKey(EncodeHintType.QR_VERSION)) {
        version = Integer.parseInt(hints.get(EncodeHintType.QR_VERSION).toString());
        int bitsNeeded = calculateBitsNeeded(mode, headerBits, dataBits, version);
        if (!willFit(bitsNeeded, version, ecLevel)) {
          throw new WriterException("Data too big for requested version");
        }
      } else {
        version = recommendVersion(ecLevel, mode, headerBits, dataBits);
      }

      headerAndDataBits = new BitArray();
      headerAndDataBits.appendBitArray(headerBits);
      // Find "length" of main segment and write it
      int numLetters = mode == Mode.BYTE ? dataBits.getSizeInBytes() : content.length();
      appendLengthInfo(numLetters, version, mode, headerAndDataBits);
      // Put data together into the overall payload
      headerAndDataBits.appendBitArray(dataBits);
    }

    int totalCodewords = totalCodewordsForVersion(version);
    int numDataBytes = totalCodewords - totalECCodewordsForVersion(version, ecLevel);

    // Terminate the bits properly.
    terminateBits(numDataBytes, headerAndDataBits);

    // Interleave data bits with error correction code.
    BitArray finalBits = interleaveWithECBytes(
        headerAndDataBits, totalCodewords, numDataBytes, numBlocksForVersion(version, ecLevel));

    // Mike-MOVED QRCode object creation from here

    //  Choose the mask pattern and set to "qrCode".
    int dimension = dimensionForVersion(version);
    ByteMatrix matrix = new ByteMatrix(dimension, dimension);

    // Enable manual selection of the pattern to be used via hint
    int maskPattern = -1;
    if (hints != null && hints.containsKey(EncodeHintType.QR_MASK_PATTERN)) {
      int hintMaskPattern = Integer.parseInt(hints.get(EncodeHintType.QR_MASK_PATTERN).toString());
      maskPattern = QRCode.isValidMaskPattern(hintMaskPattern) ? hintMaskPattern : -1;
    }

    if (maskPattern == -1) {
      maskPattern = chooseMaskPattern(finalBits, ecLevel, version, matrix);
    }

    // Build the matrix and set it to "qrCode".
    MatrixUtil.buildMatrix(finalBits, ecLevel, version, maskPattern, matrix);

    // Mike-MOVED QRCode object creation here
    return new QRCode(mode, ecLevel, version, maskPattern, matrix);
  }

  /**
   * Decides the smallest version of QR code that will contain all of the provided data.
   *
   * @throws WriterException if the data cannot fit in any version
   */
  private static int recommendVersion(ErrorCorrectionLevel ecLevel,
                                          Mode mode,
                                          BitArray headerBits,
                                          BitArray dataBits) throws WriterException {
    // Hard part: need to know version to know how many bits length takes. But need to know how many
    // bits it takes to know version. First we take a guess at version by assuming version will be
    // the minimum, 1:
    int provisionalBitsNeeded = calculateBitsNeeded(mode, headerBits, dataBits, 1);
    int provisionalVersion = chooseVersion(provisionalBitsNeeded, ecLevel);

    // Use that guess to calculate the right version. I am still not sure this works in 100% of cases.
    int bitsNeeded = calculateBitsNeeded(mode, headerBits, dataBits, provisionalVersion);
    return chooseVersion(bitsNeeded, ecLevel);
  }

  private static int calculateBitsNeeded(Mode mode, BitArray headerBits, BitArray dataBits, int version) {
    return headerBits.getSize() + mode.getCharacterCountBits(version) + dataBits.getSize();
  }

  /**
   * @return the code point of the table used in alphanumeric mode or
   *  -1 if there is no corresponding code in the table.
   */
  static int getAlphanumericCode(int code) {
    if (code < ALPHANUMERIC_TABLE.length) {
      return ALPHANUMERIC_TABLE[code];
    }
    return -1;
  }

  // Mike-REMOVED: chooseMode

  /**
   * Choose the best mode by examining the content. Note that 'encoding' is used as a hint;
   * if it is Shift_JIS, and the input is only double-byte Kanji, then we return {@link Mode#KANJI}.
   */
  static Mode chooseMode(String content, Charset encoding) { // Mike-CHANGED: unprivated
    if (SHIFT_JIS_CHARSET.equals(encoding) && isOnlyDoubleByteKanji(content)) {
      // Choose Kanji mode if all input are double-byte characters
      return Mode.KANJI;
    }
    boolean hasNumeric = false;
    boolean hasAlphanumeric = false;
    for (int i = 0; i < content.length(); ++i) {
      char c = content.charAt(i);
      if (c >= '0' && c <= '9') {
        hasNumeric = true;
      } else if (getAlphanumericCode(c) != -1) {
        hasAlphanumeric = true;
      } else {
        return Mode.BYTE;
      }
    }
    if (hasAlphanumeric) {
      return Mode.ALPHANUMERIC;
    }
    if (hasNumeric) {
      return Mode.NUMERIC;
    }
    return Mode.BYTE;
  }

  static boolean isOnlyDoubleByteKanji(String content) {
    byte[] bytes = content.getBytes(SHIFT_JIS_CHARSET);
    int length = bytes.length;
    if (length % 2 != 0) {
      return false;
    }
    for (int i = 0; i < length; i += 2) {
      int byte1 = bytes[i] & 0xFF;
      if ((byte1 < 0x81 || byte1 > 0x9F) && (byte1 < 0xE0 || byte1 > 0xEB)) {
        return false;
      }
    }
    return true;
  }

  private static int chooseMaskPattern(BitArray bits,
                                       ErrorCorrectionLevel ecLevel,
                                       int version,
                                       ByteMatrix matrix) throws WriterException {

    int minPenalty = Integer.MAX_VALUE;  // Lower penalty is better.
    int bestMaskPattern = -1;
    // We try all mask patterns to choose the best one.
    for (int maskPattern = 0; maskPattern < QRCode.NUM_MASK_PATTERNS; maskPattern++) {
      MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix);
      int penalty = calculateMaskPenalty(matrix);
      if (penalty < minPenalty) {
        minPenalty = penalty;
        bestMaskPattern = maskPattern;
      }
    }
    return bestMaskPattern;
  }

  private static int chooseVersion(int numInputBits, ErrorCorrectionLevel ecLevel) throws WriterException {
    for (int version = 1; version <= 40; version++) {
      if (willFit(numInputBits, version, ecLevel)) {
        return version;
      }
    }
    throw new WriterException("Data too big");
  }

  /**
   * @return true if the number of input bits will fit in a code with the specified version and
   * error correction level.
   */
  static boolean willFit(int numInputBits, int version, ErrorCorrectionLevel ecLevel) {
    // In the following comments, we use numbers of Version 7-H.
    // numBytes = 196
    int numBytes = totalCodewordsForVersion(version);
    // getNumECBytes = 130
    int numEcBytes = totalECCodewordsForVersion(version, ecLevel);
    // getNumDataBytes = 196 - 130 = 66
    int numDataBytes = numBytes - numEcBytes;
    int totalInputBytes = (numInputBits + 7) / 8;
    return numDataBytes >= totalInputBytes;
  }

  /**
   * Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).
   */
  static void terminateBits(int numDataBytes, BitArray bits) throws WriterException {
    int capacity = numDataBytes * 8;
    if (bits.getSize() > capacity) {
      throw new WriterException("data bits cannot fit in the QR Code" + bits.getSize() + " > " +
          capacity);
    }
    // Append Mode.TERMINATE if there is enough space (value is 0000)
    for (int i = 0; i < 4 && bits.getSize() < capacity; ++i) {
      bits.appendBit(false);
    }
    // Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
    // If the last byte isn't 8-bit aligned, we'll add padding bits.
    int numBitsInLastByte = bits.getSize() & 0x07;
    if (numBitsInLastByte > 0) {
      for (int i = numBitsInLastByte; i < 8; i++) {
        bits.appendBit(false);
      }
    }
    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
    int numPaddingBytes = numDataBytes - bits.getSizeInBytes();
    for (int i = 0; i < numPaddingBytes; ++i) {
      bits.appendBits((i & 0x01) == 0 ? 0xEC : 0x11, 8);
    }
    if (bits.getSize() != capacity) {
      throw new WriterException("Bits size does not equal capacity");
    }
  }

  /**
   * Get number of data bytes and number of error correction bytes for block id "blockID". Store
   * the result in "numDataBytesInBlock", and "numECBytesInBlock". See table 12 in 8.5.1 of
   * JISX0510:2004 (p.30)
   */
  static long getNumDataBytesAndNumECBytesForBlockID( // Mike-CHANGED return long instead of accepting two int[1]s
      int numTotalBytes, int numDataBytes, int numRSBlocks, int blockID) throws WriterException {
    if (blockID >= numRSBlocks) {
      throw new WriterException("Block ID too large");
    }
    // numRsBlocksInGroup2 = 196 % 5 = 1
    int numRsBlocksInGroup2 = numTotalBytes % numRSBlocks;
    // numRsBlocksInGroup1 = 5 - 1 = 4
    int numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2;
    // numTotalBytesInGroup1 = 196 / 5 = 39
    int numTotalBytesInGroup1 = numTotalBytes / numRSBlocks;
    // numTotalBytesInGroup2 = 39 + 1 = 40
    int numTotalBytesInGroup2 = numTotalBytesInGroup1 + 1;
    // numDataBytesInGroup1 = 66 / 5 = 13
    int numDataBytesInGroup1 = numDataBytes / numRSBlocks;
    // numDataBytesInGroup2 = 13 + 1 = 14
    int numDataBytesInGroup2 = numDataBytesInGroup1 + 1;
    // numEcBytesInGroup1 = 39 - 13 = 26
    int numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1;
    // numEcBytesInGroup2 = 40 - 14 = 26
    int numEcBytesInGroup2 = numTotalBytesInGroup2 - numDataBytesInGroup2;
    // Sanity checks.
    // 26 = 26
    if (numEcBytesInGroup1 != numEcBytesInGroup2) {
      throw new WriterException("EC bytes mismatch");
    }
    // 5 = 4 + 1.
    if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
      throw new WriterException("RS blocks mismatch");
    }
    // 196 = (13 + 26) * 4 + (14 + 26) * 1
    if (numTotalBytes !=
        ((numDataBytesInGroup1 + numEcBytesInGroup1) *
            numRsBlocksInGroup1) +
            ((numDataBytesInGroup2 + numEcBytesInGroup2) *
                numRsBlocksInGroup2)) {
      throw new WriterException("Total bytes mismatch");
    }

    return blockID < numRsBlocksInGroup1 // Mike-CHANGED return instead of assignment to array parameters
        ? (((long) numDataBytesInGroup1) << 32) | (numEcBytesInGroup1 & 0xFFFFFFFFL)
        : (((long) numDataBytesInGroup2) << 32) | (numEcBytesInGroup2 & 0xFFFFFFFFL);
  }

  /**
   * Interleave "bits" with corresponding error correction bytes. On success, store the result in
   * "result". The interleave rule is complicated. See 8.6 of JISX0510:2004 (p.37) for details.
   */
  static BitArray interleaveWithECBytes(BitArray bits,
                                        int numTotalBytes,
                                        int numDataBytes,
                                        int numRSBlocks) throws WriterException {

    // "bits" must have "getNumDataBytes" bytes of data.
    if (bits.getSizeInBytes() != numDataBytes) {
      throw new WriterException("Number of bits and data bytes does not match");
    }

    // Step 1.  Divide data bytes into blocks and generate error correction bytes for them. We'll
    // store the divided data bytes blocks and error correction bytes blocks into "blocks".
    int dataBytesOffset = 0;
    int maxNumDataBytes = 0;
    int maxNumEcBytes = 0;

    // Since, we know the number of reedsolmon blocks, we can initialize the vector with the number.
    // Mike-CHANGED using pre-sized byte[][] instead of ArrayList<struct<byte[], byte[]>>
    byte[][] blocks = new byte[2 * numRSBlocks][];

    for (int i = 0; i < numRSBlocks; ++i) {
      long pair = getNumDataBytesAndNumECBytesForBlockID(
          numTotalBytes, numDataBytes, numRSBlocks, i);
      int size = (int) (pair >>> 32);
      int numEcBytesInBlock = (int) pair;

      byte[] dataBytes = new byte[size];
      bits.toBytes(8 * dataBytesOffset, dataBytes, 0, size);
      byte[] ecBytes = generateECBytes(dataBytes, numEcBytesInBlock);
      blocks[i] = dataBytes;
      blocks[numRSBlocks + i] = ecBytes;

      maxNumDataBytes = Math.max(maxNumDataBytes, size);
      maxNumEcBytes = Math.max(maxNumEcBytes, ecBytes.length);
      dataBytesOffset += size;
    }
    if (numDataBytes != dataBytesOffset) {
      throw new WriterException("Data bytes does not match offset");
    }

    BitArray result = new BitArray();

    // First, place data blocks.
    append(result, maxNumDataBytes, blocks, 0, numRSBlocks); // Mike-CHANGED: outlined loops
    // Then, place error correction blocks.
    append(result, maxNumEcBytes, blocks, numRSBlocks, 2 * numRSBlocks);
    if (numTotalBytes != result.getSizeInBytes()) {  // Should be same.
      throw new WriterException("Interleaving error: " + numTotalBytes + " and " +
          result.getSizeInBytes() + " differ.");
    }

    return result;
  }

  // Mike-ADDED outlined method
  private static void append(BitArray result, int max, byte[][] blocks, int start, int endEx) { // Mike-ADDED
    for (int i = 0; i < max; ++i) {
      for (int j = start; j < endEx; j++) {
        byte[] bytes = blocks[j];
        if (i < bytes.length) {
          result.appendBits(bytes[i], 8);
        }
      }
    }
  }

  static byte[] generateECBytes(byte[] dataBytes, int numEcBytesInBlock) {
    int numDataBytes = dataBytes.length;
    int[] toEncode = new int[numDataBytes + numEcBytesInBlock];
    for (int i = 0; i < numDataBytes; i++) {
      toEncode[i] = dataBytes[i] & 0xFF;
    }
    // Mike-CHANGED calling static encode method
    encodeReedSolomon(QR_CODE_FIELD_256, toEncode, numEcBytesInBlock);

    byte[] ecBytes = new byte[numEcBytesInBlock];
    for (int i = 0; i < numEcBytesInBlock; i++) {
      ecBytes[i] = (byte) toEncode[numDataBytes + i];
    }
    return ecBytes;
  }

  // Mike-REMOVED appendModeInfo

  /**
   * Append length info. On success, store the result in "bits".
   */
  static void appendLengthInfo(int numLetters, int version, Mode mode, BitArray bits) throws WriterException {
    int numBits = mode.getCharacterCountBits(version);
    if (numLetters >= (1 << numBits)) {
      throw new WriterException(numLetters + " is bigger than " + ((1 << numBits) - 1));
    }
    bits.appendBits(numLetters, numBits);
  }

  /**
   * Append "bytes" in "mode" mode (encoding) into "bits". On success, store the result in "bits".
   */
  static void appendBytes( // Mike-CHANGED to accept and pass range to avoid .substring()
      String content, int from, int to, Mode mode, BitArray bits, Charset encoding) throws WriterException {
    switch (mode.ordinal()) { // Mike-CHANGED: int switch instead of enum one
      case 0: appendKanjiBytes(content.substring(from, to), bits);break;
      case 1: appendAlphanumericBytes(content, bits, from, to); break;
      case 2: appendNumericBytes(content, bits, from, to); break;
      case 3: append8BitBytes(content.substring(from, to), bits, encoding); break;
      default: throw new WriterException("Invalid mode: " + mode);
    }
  }

  // Mike-CHANGED to accept and pass range to avoid .substring()
  static void appendNumericBytes(String content, BitArray bits, int i, int length) {
    while (i < length) {
      int num1 = content.charAt(i) - '0';
      if (i + 2 < length) {
        // Encode three numeric letters in ten bits.
        int num2 = content.charAt(i + 1) - '0';
        int num3 = content.charAt(i + 2) - '0';
        bits.appendBits(num1 * 100 + num2 * 10 + num3, 10);
        i += 3;
      } else if (i + 1 < length) {
        // Encode two numeric letters in seven bits.
        int num2 = content.charAt(i + 1) - '0';
        bits.appendBits(num1 * 10 + num2, 7);
        i += 2;
      } else {
        // Encode one numeric letter in four bits.
        bits.appendBits(num1, 4);
        i++;
      }
    }
  }

  // Mike-CHANGED to accept and pass range to avoid .substring()
  static void appendAlphanumericBytes(String content, BitArray bits, int i, int length) throws WriterException {
    while (i < length) {
      int code1 = getAlphanumericCode(content.charAt(i));
      if (code1 == -1) {
        throw new WriterException();
      }
      if (i + 1 < length) {
        int code2 = getAlphanumericCode(content.charAt(i + 1));
        if (code2 == -1) {
          throw new WriterException();
        }
        // Encode two alphanumeric letters in 11 bits.
        bits.appendBits(code1 * 45 + code2, 11);
        i += 2;
      } else {
        // Encode one alphanumeric letter in six bits.
        bits.appendBits(code1, 6);
        i++;
      }
    }
  }

  static void append8BitBytes(String content, BitArray bits, Charset encoding) {
    byte[] bytes = content.getBytes(encoding);
    for (byte b : bytes) {
      bits.appendBits(b, 8);
    }
  }

  static void appendKanjiBytes(String content, BitArray bits) throws WriterException {
    byte[] bytes = content.getBytes(SHIFT_JIS_CHARSET);
    if (bytes.length % 2 != 0) {
      throw new WriterException("Kanji byte size not even");
    }
    int maxI = bytes.length - 1; // bytes.length must be even
    for (int i = 0; i < maxI; i += 2) {
      int byte1 = bytes[i] & 0xFF;
      int byte2 = bytes[i + 1] & 0xFF;
      int code = (byte1 << 8) | byte2;
      int subtracted = -1;
      if (code >= 0x8140 && code <= 0x9ffc) {
        subtracted = code - 0x8140;
      } else if (code >= 0xe040 && code <= 0xebbf) {
        subtracted = code - 0xc140;
      }
      if (subtracted == -1) {
        throw new WriterException("Invalid byte sequence");
      }
      int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
      bits.appendBits(encoded, 13);
    }
  }

  private static void appendECI(int eci, BitArray bits) { // Mike-CHANGED accept int ECI value
    bits.appendBits(Mode.ECI.getBits(), 4);
    // This is correct for values up to 127, which is all we need now.
    bits.appendBits(eci, 8);
  }

  // Mike-REMOVED BlockPair

  // Mike-MOVED from CharacterSetECI and compacted
  private static final Map<String, Integer> NAME_TO_ECI = new HashMap<>(45); static {
    NAME_TO_ECI.put("Cp437", 0);

    byte[] name1 = "ISO8859_\0\0".getBytes(), name2 = "ISO-8859-\0\0".getBytes();
    put2(name1, name2, (byte) '1', (byte) '\0', 1);
    put2(name1, name2, (byte) '2', (byte) '\0', 4);
    put2(name1, name2, (byte) '3', (byte) '\0', 5);
    put2(name1, name2, (byte) '4', (byte) '\0', 6);
    put2(name1, name2, (byte) '5', (byte) '\0', 7);
 // put2(name1, name2, (byte) '6', (byte) '\0', 8);
    put2(name1, name2, (byte) '7', (byte) '\0', 9);
 // put2(name1, name2, (byte) '8', (byte) '\0', 10);
    put2(name1, name2, (byte) '9', (byte) '\0', 11);
 // put2(name1, name2, (byte) '1', (byte) '0', 12);
 // put2(name1, name2, (byte) '1', (byte) '1', 13);
    put2(name1, name2, (byte) '1', (byte) '3', 15);
 // put2(name1, name2, (byte) '1', (byte) '4', 16);
    put2(name1, name2, (byte) '1', (byte) '5', 17);
    put2(name1, name2, (byte) '1', (byte) '6', 18);

    put2("SJIS", "Shift_JIS", 20);

    name1 = "Cp125\0\0".getBytes(); name2 = "windows-125\0\0".getBytes();
    put2(name1, name2, (byte) '0', (byte) '\0', 21);
    put2(name1, name2, (byte) '1', (byte) '\0', 22);
    put2(name1, name2, (byte) '2', (byte) '\0', 23);
    put2(name1, name2, (byte) '6', (byte) '\0', 24);

    put2("UnicodeBigUnmarked", "UTF-16BE", 25);
    NAME_TO_ECI.put("UnicodeBig", 25);

    put2("UTF8", "UTF-8", 26);

    put2("ASCII", "US-ASCII", 27);

    NAME_TO_ECI.put("Big5", 28);

    put2("GB18030", "GB2312", 29);
    put2("EUC_CN", "GBK", 29);

    put2("EUC_KR", "EUC-KR", 30);
  }
  private static void put2(byte[] template1, byte[] template2, byte postHi, byte postLo, int value) {
    put2(gen(template1, postHi, postLo), gen(template2, postHi, postLo), value);
  }
  private static String gen(byte[] template, byte postHi, byte postLo) {
    int last = template.length - 1;
    template[last-1] = postHi;
    template[last] = postLo;
    return new String(template, 0, last + (postLo == '\0' ? 0 : 1));
  }
  private static void put2(String s1, String s2, int value) {
    NAME_TO_ECI.put(s1, value);
    NAME_TO_ECI.put(s2, value);
  }
  static Integer eciByName(String name) { // Mike-CHANGED: renamed from getCharacterSetECIByName, returning 'value'
    return NAME_TO_ECI.get(name);
  }

  // Mike-MOVED from ReedSolomonEncoder
  static void encodeReedSolomon(GenericGF field, int[] toEncode, int ecBytes) {
    if (ecBytes == 0) {
      throw new IllegalArgumentException("No error correction bytes");
    }
    int dataBytes = toEncode.length - ecBytes;
    if (dataBytes <= 0) {
      throw new IllegalArgumentException("No data bytes provided");
    }

    // Mike-INLINED and simplified buildGenerator
    GenericGFPoly generator = new GenericGFPoly(field, new int[]{1});
    for (int d = 1; d <= ecBytes; d++)
      generator = generator
          .multiply(new GenericGFPoly(field, new int[] { 1, field.expTable[d - 1 + field.generatorBase]}));

    int[] infoCoefficients = new int[dataBytes];
    System.arraycopy(toEncode, 0, infoCoefficients, 0, dataBytes);
    GenericGFPoly remainder = new GenericGFPoly(field, infoCoefficients)
        .multiplyByMonomial(ecBytes, 1).remainder(generator); // Mike-CHANGED inlined local variables
    int[] coefficients = remainder.coefficients;
    int numZeroCoefficients = ecBytes - coefficients.length;
    Arrays.fill(toEncode, dataBytes, dataBytes + numZeroCoefficients, 0); // Mike-CHANGED Arrays.fill instead of a loop
    System.arraycopy(coefficients, 0, toEncode, dataBytes + numZeroCoefficients, coefficients.length);
  }
  // END Mike-MOVED

  // Mike-MOVED from Version
  // Mike-REMOVED VERSION_DECODE_INFO
  // Mike-CHANGED inlined buildVersions, flattened version data
  private static final int[] VDATA = {
      1 | 16 << 8, 1 | 19 << 8, 1 | 9 << 8, 1 | 13 << 8, 10 | 7 << 8 | 17 << 16 | 13 << 24,
      1 | 28 << 8, 1 | 34 << 8, 1 | 16 << 8, 1 | 22 << 8, 16 | 10 << 8 | 28 << 16 | 22 << 24,
      1 | 44 << 8, 1 | 55 << 8, 2 | 13 << 8, 2 | 17 << 8, 26 | 15 << 8 | 22 << 16 | 18 << 24,
      2 | 32 << 8, 1 | 80 << 8, 4 | 9 << 8, 2 | 24 << 8, 18 | 20 << 8 | 16 << 16 | 26 << 24,
      2 | 43 << 8, 1 | 108 << 8, 2 | 11 << 8 | 2 << 16 | 12 << 24, 2 | 15 << 8 | 2 << 16 | 16 << 24, 24 | 26 << 8 | 22 << 16 | 18 << 24,
      4 | 27 << 8, 2 | 68 << 8, 4 | 15 << 8, 4 | 19 << 8, 16 | 18 << 8 | 28 << 16 | 24 << 24,
      4 | 31 << 8, 2 | 78 << 8, 4 | 13 << 8 | 1 << 16 | 14 << 24, 2 | 14 << 8 | 4 << 16 | 15 << 24, 18 | 20 << 8 | 26 << 16 | 18 << 24,
      2 | 38 << 8 | 2 << 16 | 39 << 24, 2 | 97 << 8, 4 | 14 << 8 | 2 << 16 | 15 << 24, 4 | 18 << 8 | 2 << 16 | 19 << 24, 22 | 24 << 8 | 26 << 16 | 22 << 24,
      3 | 36 << 8 | 2 << 16 | 37 << 24, 2 | 116 << 8, 4 | 12 << 8 | 4 << 16 | 13 << 24, 4 | 16 << 8 | 4 << 16 | 17 << 24, 22 | 30 << 8 | 24 << 16 | 20 << 24,
      4 | 43 << 8 | 1 << 16 | 44 << 24, 2 | 68 << 8 | 2 << 16 | 69 << 24, 6 | 15 << 8 | 2 << 16 | 16 << 24, 6 | 19 << 8 | 2 << 16 | 20 << 24, 26 | 18 << 8 | 28 << 16 | 24 << 24,
      1 | 50 << 8 | 4 << 16 | 51 << 24, 4 | 81 << 8, 3 | 12 << 8 | 8 << 16 | 13 << 24, 4 | 22 << 8 | 4 << 16 | 23 << 24, 30 | 20 << 8 | 24 << 16 | 28 << 24,
      6 | 36 << 8 | 2 << 16 | 37 << 24, 2 | 92 << 8 | 2 << 16 | 93 << 24, 7 | 14 << 8 | 4 << 16 | 15 << 24, 4 | 20 << 8 | 6 << 16 | 21 << 24, 22 | 24 << 8 | 28 << 16 | 26 << 24,
      8 | 37 << 8 | 1 << 16 | 38 << 24, 4 | 107 << 8, 12 | 11 << 8 | 4 << 16 | 12 << 24, 8 | 20 << 8 | 4 << 16 | 21 << 24, 22 | 26 << 8 | 22 << 16 | 24 << 24,
      4 | 40 << 8 | 5 << 16 | 41 << 24, 3 | 115 << 8 | 1 << 16 | 116 << 24, 11 | 12 << 8 | 5 << 16 | 13 << 24, 11 | 16 << 8 | 5 << 16 | 17 << 24, 24 | 30 << 8 | 24 << 16 | 20 << 24,
      5 | 41 << 8 | 5 << 16 | 42 << 24, 5 | 87 << 8 | 1 << 16 | 88 << 24, 11 | 12 << 8 | 7 << 16 | 13 << 24, 5 | 24 << 8 | 7 << 16 | 25 << 24, 24 | 22 << 8 | 24 << 16 | 30 << 24,
      7 | 45 << 8 | 3 << 16 | 46 << 24, 5 | 98 << 8 | 1 << 16 | 99 << 24, 3 | 15 << 8 | 13 << 16 | 16 << 24, 15 | 19 << 8 | 2 << 16 | 20 << 24, 28 | 24 << 8 | 30 << 16 | 24 << 24,
      10 | 46 << 8 | 1 << 16 | 47 << 24, 1 | 107 << 8 | 5 << 16 | 108 << 24, 2 | 14 << 8 | 17 << 16 | 15 << 24, 1 | 22 << 8 | 15 << 16 | 23 << 24, 28 | 28 << 8 | 28 << 16 | 28 << 24,
      9 | 43 << 8 | 4 << 16 | 44 << 24, 5 | 120 << 8 | 1 << 16 | 121 << 24, 2 | 14 << 8 | 19 << 16 | 15 << 24, 17 | 22 << 8 | 1 << 16 | 23 << 24, 26 | 30 << 8 | 28 << 16 | 28 << 24,
      3 | 44 << 8 | 11 << 16 | 45 << 24, 3 | 113 << 8 | 4 << 16 | 114 << 24, 9 | 13 << 8 | 16 << 16 | 14 << 24, 17 | 21 << 8 | 4 << 16 | 22 << 24, 26 | 28 << 8 | 26 << 16 | 26 << 24,
      3 | 41 << 8 | 13 << 16 | 42 << 24, 3 | 107 << 8 | 5 << 16 | 108 << 24, 15 | 15 << 8 | 10 << 16 | 16 << 24, 15 | 24 << 8 | 5 << 16 | 25 << 24, 26 | 28 << 8 | 28 << 16 | 30 << 24,
      17 | 42 << 8, 4 | 116 << 8 | 4 << 16 | 117 << 24, 19 | 16 << 8 | 6 << 16 | 17 << 24, 17 | 22 << 8 | 6 << 16 | 23 << 24, 26 | 28 << 8 | 30 << 16 | 28 << 24,
      17 | 46 << 8, 2 | 111 << 8 | 7 << 16 | 112 << 24, 34 | 13 << 8, 7 | 24 << 8 | 16 << 16 | 25 << 24, 28 | 28 << 8 | 24 << 16 | 30 << 24,
      4 | 47 << 8 | 14 << 16 | 48 << 24, 4 | 121 << 8 | 5 << 16 | 122 << 24, 16 | 15 << 8 | 14 << 16 | 16 << 24, 11 | 24 << 8 | 14 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      6 | 45 << 8 | 14 << 16 | 46 << 24, 6 | 117 << 8 | 4 << 16 | 118 << 24, 30 | 16 << 8 | 2 << 16 | 17 << 24, 11 | 24 << 8 | 16 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      8 | 47 << 8 | 13 << 16 | 48 << 24, 8 | 106 << 8 | 4 << 16 | 107 << 24, 22 | 15 << 8 | 13 << 16 | 16 << 24, 7 | 24 << 8 | 22 << 16 | 25 << 24, 28 | 26 << 8 | 30 << 16 | 30 << 24,
      19 | 46 << 8 | 4 << 16 | 47 << 24, 10 | 114 << 8 | 2 << 16 | 115 << 24, 33 | 16 << 8 | 4 << 16 | 17 << 24, 28 | 22 << 8 | 6 << 16 | 23 << 24, 28 | 28 << 8 | 30 << 16 | 28 << 24,
      22 | 45 << 8 | 3 << 16 | 46 << 24, 8 | 122 << 8 | 4 << 16 | 123 << 24, 12 | 15 << 8 | 28 << 16 | 16 << 24, 8 | 23 << 8 | 26 << 16 | 24 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      3 | 45 << 8 | 23 << 16 | 46 << 24, 3 | 117 << 8 | 10 << 16 | 118 << 24, 11 | 15 << 8 | 31 << 16 | 16 << 24, 4 | 24 << 8 | 31 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      21 | 45 << 8 | 7 << 16 | 46 << 24, 7 | 116 << 8 | 7 << 16 | 117 << 24, 19 | 15 << 8 | 26 << 16 | 16 << 24, 1 | 23 << 8 | 37 << 16 | 24 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      19 | 47 << 8 | 10 << 16 | 48 << 24, 5 | 115 << 8 | 10 << 16 | 116 << 24, 23 | 15 << 8 | 25 << 16 | 16 << 24, 15 | 24 << 8 | 25 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      2 | 46 << 8 | 29 << 16 | 47 << 24, 13 | 115 << 8 | 3 << 16 | 116 << 24, 23 | 15 << 8 | 28 << 16 | 16 << 24, 42 | 24 << 8 | 1 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      10 | 46 << 8 | 23 << 16 | 47 << 24, 17 | 115 << 8, 19 | 15 << 8 | 35 << 16 | 16 << 24, 10 | 24 << 8 | 35 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      14 | 46 << 8 | 21 << 16 | 47 << 24, 17 | 115 << 8 | 1 << 16 | 116 << 24, 11 | 15 << 8 | 46 << 16 | 16 << 24, 29 | 24 << 8 | 19 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      14 | 46 << 8 | 23 << 16 | 47 << 24, 13 | 115 << 8 | 6 << 16 | 116 << 24, 59 | 16 << 8 | 1 << 16 | 17 << 24, 44 | 24 << 8 | 7 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      12 | 47 << 8 | 26 << 16 | 48 << 24, 12 | 121 << 8 | 7 << 16 | 122 << 24, 22 | 15 << 8 | 41 << 16 | 16 << 24, 39 | 24 << 8 | 14 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      6 | 47 << 8 | 34 << 16 | 48 << 24, 6 | 121 << 8 | 14 << 16 | 122 << 24, 2 | 15 << 8 | 64 << 16 | 16 << 24, 46 | 24 << 8 | 10 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      29 | 46 << 8 | 14 << 16 | 47 << 24, 17 | 122 << 8 | 4 << 16 | 123 << 24, 24 | 15 << 8 | 46 << 16 | 16 << 24, 49 | 24 << 8 | 10 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      13 | 46 << 8 | 32 << 16 | 47 << 24, 4 | 122 << 8 | 18 << 16 | 123 << 24, 42 | 15 << 8 | 32 << 16 | 16 << 24, 48 | 24 << 8 | 14 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      40 | 47 << 8 | 7 << 16 | 48 << 24, 20 | 117 << 8 | 4 << 16 | 118 << 24, 10 | 15 << 8 | 67 << 16 | 16 << 24, 43 | 24 << 8 | 22 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
      18 | 47 << 8 | 31 << 16 | 48 << 24, 19 | 118 << 8 | 6 << 16 | 119 << 24, 20 | 15 << 8 | 61 << 16 | 16 << 24, 34 | 24 << 8 | 34 << 16 | 25 << 24, 28 | 30 << 8 | 30 << 16 | 30 << 24,
  };
  // Mike-REMOVED versionNumber, alignmentPatternCenters, ecBlocks, totalCodewords,
  // constructor, getVersionNumber, getAlignmentPatternCenters
  // Mike-REWORKED instance methods to be static
  public static int totalCodewordsForVersion(int versionNumber) {
    int offset = 5 * (versionNumber - 1);
    int ecCodewords = VDATA[offset + 4] >>> 8 & 0xFF;
    int ecbArray = VDATA[offset + 1];
    return (ecbArray & 0xFF) * ((ecbArray >>> 8 & 0xFF) + ecCodewords) +
        (ecbArray >>> 16 & 0xFF) * ((ecbArray >>> 24 & 0xFF) + ecCodewords);
  }
  public static int dimensionForVersion(int versionNumber) {
    return 17 + 4 * versionNumber;
  }
  public static int totalECCodewordsForVersion(int versionNumber, ErrorCorrectionLevel ecLevel) {
    return ((VDATA[5*(versionNumber-1)+4] >>> (8*ecLevel.ordinal())) & 0xFF) * numBlocksForVersion(versionNumber, ecLevel);
  }
  public static int numBlocksForVersion(int versionNumber, ErrorCorrectionLevel ecLevel) {
    int ecBlocks = VDATA[5 * (versionNumber - 1) + ecLevel.ordinal()];
    return (ecBlocks & 0xFF) + (ecBlocks >>> 16 & 0xFF);
  }
  // Mike-REMOVED getProvisionalVersionForDimension, getVersionForNumber, decodeVersionInformation, buildFunctionPattern
  // Mike-REMOVED ECBlocks, ECB
  // Mike-REMOVED toString, buildVersions
  // END Mike-MOVED

}
