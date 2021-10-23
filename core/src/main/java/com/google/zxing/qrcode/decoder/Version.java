/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.qrcode.decoder;

/**
 * See ISO 18004:2006 Annex D
 *
 * @author Sean Owen
 */
public final class Version {

  // Mike-REMOVED VERSION_DECODE_INFO

  // Mike-CHANGED version data storage format
  private static final Version[] VERSIONS = new Version[]{
      new Version(1, new ECBlocks(7, 1, 19), new ECBlocks(10, 1, 16), new ECBlocks(13, 1, 13), new ECBlocks(17, 1, 9)),
      new Version(2, new ECBlocks(10, 1, 34), new ECBlocks(16, 1, 28), new ECBlocks(22, 1, 22), new ECBlocks(28, 1, 16)),
      new Version(3, new ECBlocks(15, 1, 55), new ECBlocks(26, 1, 44), new ECBlocks(18, 2, 17), new ECBlocks(22, 2, 13)),
      new Version(4, new ECBlocks(20, 1, 80), new ECBlocks(18, 2, 32), new ECBlocks(26, 2, 24), new ECBlocks(16, 4, 9)),
      new Version(5, new ECBlocks(26, 1, 108), new ECBlocks(24, 2, 43), new ECBlocks(18, 2, 15, 2, 16), new ECBlocks(22, 2, 11, 2, 12)),
      new Version(6, new ECBlocks(18, 2, 68), new ECBlocks(16, 4, 27), new ECBlocks(24, 4, 19), new ECBlocks(28, 4, 15)),
      new Version(7, new ECBlocks(20, 2, 78), new ECBlocks(18, 4, 31), new ECBlocks(18, 2, 14, 4, 15), new ECBlocks(26, 4, 13, 1, 14)),
      new Version(8, new ECBlocks(24, 2, 97), new ECBlocks(22, 2, 38, 2, 39), new ECBlocks(22, 4, 18, 2, 19), new ECBlocks(26, 4, 14, 2, 15)),
      new Version(9, new ECBlocks(30, 2, 116), new ECBlocks(22, 3, 36, 2, 37), new ECBlocks(20, 4, 16, 4, 17), new ECBlocks(24, 4, 12, 4, 13)),
      new Version(10, new ECBlocks(18, 2, 68, 2, 69), new ECBlocks(26, 4, 43, 1, 44), new ECBlocks(24, 6, 19, 2, 20), new ECBlocks(28, 6, 15, 2, 16)),
      new Version(11, new ECBlocks(20, 4, 81), new ECBlocks(30, 1, 50, 4, 51), new ECBlocks(28, 4, 22, 4, 23), new ECBlocks(24, 3, 12, 8, 13)),
      new Version(12, new ECBlocks(24, 2, 92, 2, 93), new ECBlocks(22, 6, 36, 2, 37), new ECBlocks(26, 4, 20, 6, 21), new ECBlocks(28, 7, 14, 4, 15)),
      new Version(13, new ECBlocks(26, 4, 107), new ECBlocks(22, 8, 37, 1, 38), new ECBlocks(24, 8, 20, 4, 21), new ECBlocks(22, 12, 11, 4, 12)),
      new Version(14, new ECBlocks(30, 3, 115, 1, 116), new ECBlocks(24, 4, 40, 5, 41), new ECBlocks(20, 11, 16, 5, 17), new ECBlocks(24, 11, 12, 5, 13)),
      new Version(15, new ECBlocks(22, 5, 87, 1, 88), new ECBlocks(24, 5, 41, 5, 42), new ECBlocks(30, 5, 24, 7, 25), new ECBlocks(24, 11, 12, 7, 13)),
      new Version(16, new ECBlocks(24, 5, 98, 1, 99), new ECBlocks(28, 7, 45, 3, 46), new ECBlocks(24, 15, 19, 2, 20), new ECBlocks(30, 3, 15, 13, 16)),
      new Version(17, new ECBlocks(28, 1, 107, 5, 108), new ECBlocks(28, 10, 46, 1, 47), new ECBlocks(28, 1, 22, 15, 23), new ECBlocks(28, 2, 14, 17, 15)),
      new Version(18, new ECBlocks(30, 5, 120, 1, 121), new ECBlocks(26, 9, 43, 4, 44), new ECBlocks(28, 17, 22, 1, 23), new ECBlocks(28, 2, 14, 19, 15)),
      new Version(19, new ECBlocks(28, 3, 113, 4, 114), new ECBlocks(26, 3, 44, 11, 45), new ECBlocks(26, 17, 21, 4, 22), new ECBlocks(26, 9, 13, 16, 14)),
      new Version(20, new ECBlocks(28, 3, 107, 5, 108), new ECBlocks(26, 3, 41, 13, 42), new ECBlocks(30, 15, 24, 5, 25), new ECBlocks(28, 15, 15, 10, 16)),
      new Version(21, new ECBlocks(28, 4, 116, 4, 117), new ECBlocks(26, 17, 42), new ECBlocks(28, 17, 22, 6, 23), new ECBlocks(30, 19, 16, 6, 17)),
      new Version(22, new ECBlocks(28, 2, 111, 7, 112), new ECBlocks(28, 17, 46), new ECBlocks(30, 7, 24, 16, 25), new ECBlocks(24, 34, 13)),
      new Version(23, new ECBlocks(30, 4, 121, 5, 122), new ECBlocks(28, 4, 47, 14, 48), new ECBlocks(30, 11, 24, 14, 25), new ECBlocks(30, 16, 15, 14, 16)),
      new Version(24, new ECBlocks(30, 6, 117, 4, 118), new ECBlocks(28, 6, 45, 14, 46), new ECBlocks(30, 11, 24, 16, 25), new ECBlocks(30, 30, 16, 2, 17)),
      new Version(25, new ECBlocks(26, 8, 106, 4, 107), new ECBlocks(28, 8, 47, 13, 48), new ECBlocks(30, 7, 24, 22, 25), new ECBlocks(30, 22, 15, 13, 16)),
      new Version(26, new ECBlocks(28, 10, 114, 2, 115), new ECBlocks(28, 19, 46, 4, 47), new ECBlocks(28, 28, 22, 6, 23), new ECBlocks(30, 33, 16, 4, 17)),
      new Version(27, new ECBlocks(30, 8, 122, 4, 123), new ECBlocks(28, 22, 45, 3, 46), new ECBlocks(30, 8, 23, 26, 24), new ECBlocks(30, 12, 15, 28, 16)),
      new Version(28, new ECBlocks(30, 3, 117, 10, 118), new ECBlocks(28, 3, 45, 23, 46), new ECBlocks(30, 4, 24, 31, 25), new ECBlocks(30, 11, 15, 31, 16)),
      new Version(29, new ECBlocks(30, 7, 116, 7, 117), new ECBlocks(28, 21, 45, 7, 46), new ECBlocks(30, 1, 23, 37, 24), new ECBlocks(30, 19, 15, 26, 16)),
      new Version(30, new ECBlocks(30, 5, 115, 10, 116), new ECBlocks(28, 19, 47, 10, 48), new ECBlocks(30, 15, 24, 25, 25), new ECBlocks(30, 23, 15, 25, 16)),
      new Version(31, new ECBlocks(30, 13, 115, 3, 116), new ECBlocks(28, 2, 46, 29, 47), new ECBlocks(30, 42, 24, 1, 25), new ECBlocks(30, 23, 15, 28, 16)),
      new Version(32, new ECBlocks(30, 17, 115), new ECBlocks(28, 10, 46, 23, 47), new ECBlocks(30, 10, 24, 35, 25), new ECBlocks(30, 19, 15, 35, 16)),
      new Version(33, new ECBlocks(30, 17, 115, 1, 116), new ECBlocks(28, 14, 46, 21, 47), new ECBlocks(30, 29, 24, 19, 25), new ECBlocks(30, 11, 15, 46, 16)),
      new Version(34, new ECBlocks(30, 13, 115, 6, 116), new ECBlocks(28, 14, 46, 23, 47), new ECBlocks(30, 44, 24, 7, 25), new ECBlocks(30, 59, 16, 1, 17)),
      new Version(35, new ECBlocks(30, 12, 121, 7, 122), new ECBlocks(28, 12, 47, 26, 48), new ECBlocks(30, 39, 24, 14, 25), new ECBlocks(30, 22, 15, 41, 16)),
      new Version(36, new ECBlocks(30, 6, 121, 14, 122), new ECBlocks(28, 6, 47, 34, 48), new ECBlocks(30, 46, 24, 10, 25), new ECBlocks(30, 2, 15, 64, 16)),
      new Version(37, new ECBlocks(30, 17, 122, 4, 123), new ECBlocks(28, 29, 46, 14, 47), new ECBlocks(30, 49, 24, 10, 25), new ECBlocks(30, 24, 15, 46, 16)),
      new Version(38, new ECBlocks(30, 4, 122, 18, 123), new ECBlocks(28, 13, 46, 32, 47), new ECBlocks(30, 48, 24, 14, 25), new ECBlocks(30, 42, 15, 32, 16)),
      new Version(39, new ECBlocks(30, 20, 117, 4, 118), new ECBlocks(28, 40, 47, 7, 48), new ECBlocks(30, 43, 24, 22, 25), new ECBlocks(30, 10, 15, 67, 16)),
      new Version(40, new ECBlocks(30, 19, 118, 6, 119), new ECBlocks(28, 18, 47, 31, 48), new ECBlocks(30, 34, 24, 34, 25), new ECBlocks(30, 20, 15, 61, 16))
  };

  private final int versionNumber;
  // Mike-REMOVED alignmentPatternCenters
  private final ECBlocks[] ecBlocks;
  private final int totalCodewords;

  private Version(int versionNumber, ECBlocks... ecBlocks) {
    this.versionNumber = versionNumber;
    this.ecBlocks = new ECBlocks[] { ecBlocks[1], ecBlocks[0], ecBlocks[3], ecBlocks[2] };
    int ecCodewords = ecBlocks[1].ecCodewordsPerBlock;
    int ecbArray = ecBlocks[1].ecBlocks;
    this.totalCodewords =
        ECB.count1(ecbArray) * (ECB.codeWords1(ecbArray) + ecCodewords) +
            ECB.count2(ecbArray) * (ECB.codeWords2(ecbArray) + ecCodewords);
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  // Mike-REMOVED getAlignmentPatternCenters

  public int getTotalCodewords() {
    return totalCodewords;
  }

  public int getDimensionForVersion() {
    return 17 + 4 * versionNumber;
  }

  public ECBlocks getECBlocksForLevel(ErrorCorrectionLevel ecLevel) {
    return ecBlocks[ecLevel.ordinal()];
  }

  // Mike-REMOVED getProvisionalVersionForDimension

  public static Version getVersionForNumber(int versionNumber) {
    if (versionNumber < 1 || versionNumber > 40) {
      throw new IllegalArgumentException();
    }
    return VERSIONS[versionNumber - 1];
  }

  // Mike-REMOVED decodeVersionInformation, buildFunctionPattern

  /**
   * <p>Encapsulates a set of error-correction blocks in one symbol version. Most versions will
   * use blocks of differing sizes within one version, so, this encapsulates the parameters for
   * each set of blocks. It also holds the number of error-correction codewords per block since it
   * will be the same across all blocks within one version.</p>
   */
  public static final class ECBlocks {
    // Mike-CHANGED unprivated
    final int ecCodewordsPerBlock;
    final int ecBlocks; // Mike-CHANGED ECB[] => int

    // Mike-CHANGED constructors to accept ints
    ECBlocks(int ecCodewordsPerBlock, int ecb1count, int ecb1CodeWords) {
      this(ecCodewordsPerBlock, ecb1count, ecb1CodeWords, 0, 0);
    }
    ECBlocks(int ecCodewordsPerBlock, int ecb1count, int ecb1CodeWords, int ecb2Count, int ecb2CodeWords) {
      this.ecCodewordsPerBlock = ecCodewordsPerBlock;
      this.ecBlocks = ecb1count | (ecb1CodeWords << 8) | (ecb2Count << 16) | (ecb2CodeWords << 24);
    }

    // Mike-REMOVED getECCodewordsPerBlock

    public int getNumBlocks() {
      return ECB.count1(ecBlocks) + ECB.count2(ecBlocks);
    }

    public int getTotalECCodewords() {
      return ecCodewordsPerBlock * getNumBlocks();
    }

    // Mike-REMOVED getECBlocks
  }

  /**
   * <p>Encapsulates the parameters for one error-correction block in one symbol version.
   * This includes the number of data codewords, and the number of times a block with these
   * parameters is used consecutively in the QR code version's format.</p>
   */
  private static final class ECB { // Mike-CHANGED: privated
    // Mike-REPLACED class contents to be a set of static utilities
    static int count1(int ecb) { return ecb & 0xFF; }
    static int codeWords1(int ecb) { return (ecb >>> 8) & 0xFF; }
    static int count2(int ecb) { return (ecb >>> 16) & 0xFF; }
    static int codeWords2(int ecb) { return ecb >>> 24; }
  }

  @Override
  public String toString() {
    return String.valueOf(versionNumber);
  }

  // Mike-INLINED buildVersions

}
