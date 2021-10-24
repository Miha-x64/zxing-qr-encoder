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

import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
final class MatrixUtil {

  // Mike-CHANGED: compacted arrays
  private static final long POSITION_DETECTION_PATTERN =
      0b1111111L << 42 |
      0b1000001L << 35 |
      0b1011101L << 28 |
      0b1011101L << 21 |
      0b1011101L << 14 |
      0b1000001L << 7 |
      0b1111111L;

  private static final int POSITION_ADJUSTMENT_PATTERN =
      0b11111 << 20 |
      0b10001 << 15 |
      0b10101 << 10 |
      0b10001 << 5 |
      0b11111;

  // Type info cells at the left top corner.
  private static final long TYPE_INFO_COORDINATES =
      (8L << 56) | (8L << 52) | (8L << 48) | (8L << 44) | (8L << 40) | (8L << 36) | (8L << 32) |
          (8L << 28) | (7L << 24) | (5L << 20) | (4L << 16) | (3L << 12) | (2L << 8) | (1L << 4) /* | 0 */;
  // END Mike-CHANGED array compaction

  // From Appendix D in JISX0510:2004 (p. 67)
  private static final int VERSION_INFO_POLY = 0x1f25;  // 1 1111 0010 0101

  // From Appendix C in JISX0510:2004 (p.65).
  private static final int TYPE_INFO_POLY = 0x537;
  private static final int TYPE_INFO_MASK_PATTERN = 0x5412;

  private MatrixUtil() {
    // do nothing
  }

  // Set all cells to -1.  -1 means that the cell is empty (not set yet).
  //
  // JAVAPORT: We shouldn't need to do this at all. The code should be rewritten to begin encoding
  // with the ByteMatrix initialized all to zero.
  static void clearMatrix(ByteMatrix matrix) {
    matrix.clear((byte) -1);
  }

  // Build 2D matrix of QR Code from "dataBits" with "ecLevel", "version" and "getMaskPattern". On
  // success, store the result in "matrix" and return true.
  static void buildMatrix(BitArray dataBits,
                          ErrorCorrectionLevel ecLevel,
                          int version,
                          int maskPattern,
                          ByteMatrix matrix) throws WriterException {
    clearMatrix(matrix);
    embedBasicPatterns(version, matrix);
    // Type information appear with any version.
    embedTypeInfo(ecLevel, maskPattern, matrix);
    // Version info appear if version >= 7.
    maybeEmbedVersionInfo(version, matrix);
    // Data should be embedded at end.
    embedDataBits(dataBits, maskPattern, matrix);
  }

  // Embed basic patterns. On success, modify the matrix and return true.
  // The basic patterns are:
  // - Position detection patterns
  // - Timing patterns
  // - Dark dot at the left bottom corner
  // - Position adjustment patterns, if need be
  static void embedBasicPatterns(int version, ByteMatrix matrix) throws WriterException {
    // Let's get started with embedding big squares at corners.
    embedPositionDetectionPatternsAndSeparators(matrix);

    // Embed the lonely dark dot at left bottom corner. JISX0510:2004 (p.46)
    // Mike-CHANGED: inlined embedDarkDotAtLeftBottomCorner()
    if (matrix.get(8, matrix.height - 8) == 0) throw new WriterException();
    matrix.set(8, matrix.height - 8, 1);

    // Position adjustment patterns appear if version >= 2.
    maybeEmbedPositionAdjustmentPatterns(version, matrix);
    // Timing patterns should be embedded after position adj. patterns.
    embedTimingPatterns(matrix);
  }

  // Embed type information. On success, modify the matrix.
  static void embedTypeInfo(ErrorCorrectionLevel ecLevel, int maskPattern, ByteMatrix matrix)
      throws WriterException {
    BitArray typeInfoBits = new BitArray();
    makeTypeInfoBits(ecLevel, maskPattern, typeInfoBits);

    long typeInfoCoordinateX = TYPE_INFO_COORDINATES;
    long typeInfoCoordinateY = TYPE_INFO_COORDINATES;
    for (int i = 0; i < 15; ++i) {
      // Place bits in LSB to MSB order.  LSB (least significant bit) is the last value in
      // "typeInfoBits".
      boolean bit = typeInfoBits.get(typeInfoBits.getSize() - 1 - i);

      // Type info bits at the left top corner. See 8.9 of JISX0510:2004 (p.46).
      matrix.set((int) ((typeInfoCoordinateX >>> 56) & 0xF), (int) (typeInfoCoordinateY & 0xF), bit);
      typeInfoCoordinateX <<= 4;
      typeInfoCoordinateY >>>= 4;

      int x2;
      int y2;
      if (i < 8) {
        // Right top corner.
        x2 = matrix.width - i - 1;
        y2 = 8;
      } else {
        // Left bottom corner.
        x2 = 8;
        y2 = matrix.height - 7 + (i - 8);
      }
      matrix.set(x2, y2, bit);
    }
  }

  // Embed version information if need be. On success, modify the matrix and return true.
  // See 8.10 of JISX0510:2004 (p.47) for how to embed version information.
  static void maybeEmbedVersionInfo(int version, ByteMatrix matrix) throws WriterException {
    if (version < 7) {  // Version info is necessary if version >= 7.
      return;  // Don't need version info.
    }
    BitArray versionInfoBits = new BitArray();
    makeVersionInfoBits(version, versionInfoBits);

    int bitIndex = 6 * 3 - 1;  // It will decrease from 17 to 0.
    for (int i = 0; i < 6; ++i) {
      for (int j = 0; j < 3; ++j) {
        // Place bits in LSB (least significant bit) to MSB order.
        boolean bit = versionInfoBits.get(bitIndex);
        bitIndex--;
        // Left bottom corner.
        matrix.set(i, matrix.height - 11 + j, bit);
        // Right bottom corner.
        matrix.set(matrix.height - 11 + j, i, bit);
      }
    }
  }

  // Embed "dataBits" using "getMaskPattern". On success, modify the matrix and return true.
  // For debugging purposes, it skips masking process if "getMaskPattern" is -1.
  // See 8.7 of JISX0510:2004 (p.38) for how to embed data bits.
  static void embedDataBits(BitArray dataBits, int maskPattern, ByteMatrix matrix)
      throws WriterException {
    int bitIndex = 0;
    int direction = -1;
    // Start from the right bottom cell.
    int x = matrix.width - 1;
    int y = matrix.height - 1;
    while (x > 0) {
      // Skip the vertical timing pattern.
      if (x == 6) {
        x -= 1;
      }
      while (y >= 0 && y < matrix.height) {
        for (int i = 0; i < 2; ++i) {
          int xx = x - i;
          // Skip the cell if it's not empty.
          if (!isEmpty(matrix.get(xx, y))) {
            continue;
          }
          boolean bit;
          if (bitIndex < dataBits.getSize()) {
            bit = dataBits.get(bitIndex);
            ++bitIndex;
          } else {
            // Padding bit. If there is no bit left, we'll fill the left cells with 0, as described
            // in 8.4.9 of JISX0510:2004 (p. 24).
            bit = false;
          }

          // Skip masking if mask_pattern is -1.
          if (maskPattern != -1 && MaskUtil.getDataMaskBit(maskPattern, xx, y)) {
            bit = !bit;
          }
          matrix.set(xx, y, bit);
        }
        y += direction;
      }
      direction = -direction;  // Reverse the direction.
      y += direction;
      x -= 2;  // Move to the left.
    }
    // All bits should be consumed.
    if (bitIndex != dataBits.getSize()) {
      throw new WriterException("Not all bits consumed: " + bitIndex + '/' + dataBits.getSize());
    }
  }

  // Mike-REMOVED findMSBSet

  // Calculate BCH (Bose-Chaudhuri-Hocquenghem) code for "value" using polynomial "poly". The BCH
  // code is used for encoding type information and version information.
  // Example: Calculation of version information of 7.
  // f(x) is created from 7.
  //   - 7 = 000111 in 6 bits
  //   - f(x) = x^2 + x^1 + x^0
  // g(x) is given by the standard (p. 67)
  //   - g(x) = x^12 + x^11 + x^10 + x^9 + x^8 + x^5 + x^2 + 1
  // Multiply f(x) by x^(18 - 6)
  //   - f'(x) = f(x) * x^(18 - 6)
  //   - f'(x) = x^14 + x^13 + x^12
  // Calculate the remainder of f'(x) / g(x)
  //         x^2
  //         __________________________________________________
  //   g(x) )x^14 + x^13 + x^12
  //         x^14 + x^13 + x^12 + x^11 + x^10 + x^7 + x^4 + x^2
  //         --------------------------------------------------
  //                              x^11 + x^10 + x^7 + x^4 + x^2
  //
  // The remainder is x^11 + x^10 + x^7 + x^4 + x^2
  // Encode it in binary: 110010010100
  // The return value is 0xc94 (1100 1001 0100)
  //
  // Since all coefficients in the polynomials are 1 or 0, we can do the calculation by bit
  // operations. We don't care if coefficients are positive or negative.
  static int calculateBCHCode(int value, int poly) {
    if (poly == 0) {
      throw new IllegalArgumentException("0 polynomial");
    }
    // If poly is "1 1111 0010 0101" (version info poly), msbSetInPoly is 13. We'll subtract 1
    // from 13 to make it 12.
    int msbSetInPoly = 32 - Integer.numberOfLeadingZeros(poly); // Mike-CHANGED: inlined findMSBSet
    value <<= msbSetInPoly - 1;
    // Do the division business using exclusive-or operations.
    while (32 - Integer.numberOfLeadingZeros(value) >= msbSetInPoly) {
      value ^= poly << (32 - Integer.numberOfLeadingZeros(value) - msbSetInPoly);
    }
    // Now the "value" is the remainder (i.e. the BCH code)
    return value;
  }

  // Make bit vector of type information. On success, store the result in "bits" and return true.
  // Encode error correction level and mask pattern. See 8.9 of
  // JISX0510:2004 (p.45) for details.
  static void makeTypeInfoBits(ErrorCorrectionLevel ecLevel, int maskPattern, BitArray bits)
      throws WriterException {
    if (!QRCode.isValidMaskPattern(maskPattern)) {
      throw new WriterException("Invalid mask pattern");
    }
    int typeInfo = (ecLevel.ordinal() << 3) | maskPattern;
    bits.appendBits(typeInfo, 5);

    int bchCode = calculateBCHCode(typeInfo, TYPE_INFO_POLY);
    bits.appendBits(bchCode, 10);

    BitArray maskBits = new BitArray();
    maskBits.appendBits(TYPE_INFO_MASK_PATTERN, 15);
    bits.xor(maskBits);

    if (bits.getSize() != 15) {  // Just in case.
      throw new WriterException("should not happen but we got: " + bits.getSize());
    }
  }

  // Make bit vector of version information. On success, store the result in "bits" and return true.
  // See 8.10 of JISX0510:2004 (p.45) for details.
  static void makeVersionInfoBits(int version, BitArray bits) throws WriterException {
    bits.appendBits(version, 6);
    int bchCode = calculateBCHCode(version, VERSION_INFO_POLY);
    bits.appendBits(bchCode, 12);

    if (bits.getSize() != 18) {  // Just in case.
      throw new WriterException("should not happen but we got: " + bits.getSize());
    }
  }

  // Check if "value" is empty.
  private static boolean isEmpty(int value) {
    return value == -1;
  }

  private static void embedTimingPatterns(ByteMatrix matrix) {
    // -8 is for skipping position detection patterns (size 7), and two horizontal/vertical
    // separation patterns (size 1). Thus, 8 = 7 + 1.
    for (int i = 8; i < matrix.width - 8; ++i) {
      int bit = (i + 1) % 2;
      // Horizontal line.
      if (isEmpty(matrix.get(i, 6))) {
        matrix.set(i, 6, bit);
      }
      // Vertical line.
      if (isEmpty(matrix.get(6, i))) {
        matrix.set(6, i, bit);
      }
    }
  }

  // Mike-REMOVED embedDarkDotAtLeftBottomCorner

  private static void embedHorizontalSeparationPattern(int xStart,
                                                       int yStart,
                                                       ByteMatrix matrix) throws WriterException {
    for (int x = 0; x < 8; ++x) {
      if (!isEmpty(matrix.get(xStart + x, yStart))) {
        throw new WriterException();
      }
      matrix.set(xStart + x, yStart, 0);
    }
  }

  private static void embedVerticalSeparationPattern(int xStart,
                                                     int yStart,
                                                     ByteMatrix matrix) throws WriterException {
    for (int y = 0; y < 7; ++y) {
      if (!isEmpty(matrix.get(xStart, yStart + y))) {
        throw new WriterException();
      }
      matrix.set(xStart, yStart + y, 0);
    }
  }

  // Mike-CHANGED: generalized embedPositionAdjustmentPattern and embedPositionDetectionPattern
  private static void embedPattern(int xStart, int yStart, ByteMatrix matrix, long pattern, int size) {
    for (int y = 0; y < size; ++y) {
      for (int x = 0; x < size; ++x) {
        matrix.set(xStart + x, yStart + y, (int) (pattern & 1));
        pattern >>>= 1;
      }
    }
  }

  // Embed position detection patterns and surrounding vertical/horizontal separators.
  private static void embedPositionDetectionPatternsAndSeparators(ByteMatrix matrix) throws WriterException {
    // Embed three big squares at corners.
    // Left top corner.
    embedPattern(0, 0, matrix, POSITION_DETECTION_PATTERN, 7);
    // Right top corner.
    embedPattern(matrix.width - 7, 0, matrix, POSITION_DETECTION_PATTERN, 7);
    // Left bottom corner.
    embedPattern(0, matrix.width - 7, matrix, POSITION_DETECTION_PATTERN, 7);

    // Embed horizontal separation patterns around the squares.
    int hspWidth = 8;
    // Left top corner.
    embedHorizontalSeparationPattern(0, hspWidth - 1, matrix);
    // Right top corner.
    embedHorizontalSeparationPattern(matrix.width - hspWidth,
        hspWidth - 1, matrix);
    // Left bottom corner.
    embedHorizontalSeparationPattern(0, matrix.width - hspWidth, matrix);

    // Embed vertical separation patterns around the squares.
    int vspSize = 7;
    // Left top corner.
    embedVerticalSeparationPattern(vspSize, 0, matrix);
    // Right top corner.
    embedVerticalSeparationPattern(matrix.height - vspSize - 1, 0, matrix);
    // Left bottom corner.
    embedVerticalSeparationPattern(vspSize, matrix.height - vspSize,
        matrix);
  }

  // Embed position adjustment patterns if need be.
  private static void maybeEmbedPositionAdjustmentPatterns(int version, ByteMatrix matrix) {
    if (version < 2) {  // The patterns appear if version >= 2
      return;
    }
    int offset = 7 * (version - 1);
    for (int i = 0; i < 7; i++) {
      int y = Encoder.POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE[offset + i] & 0xFF;
      if (y != 255) {
        for (int j = 0; j < 7; j++) {
          int x = Encoder.POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE[offset + j] & 0xFF;
          if (x != 255 && isEmpty(matrix.get(x, y))) {
            // If the cell is unset, we embed the position adjustment pattern here.
            // -2 is necessary since the x/y coordinates point to the center of the pattern, not the
            // left top corner.
            embedPattern(x - 2, y - 2, matrix, POSITION_ADJUSTMENT_PATTERN, 5);
          }
        }
      }
    }
  }

}
