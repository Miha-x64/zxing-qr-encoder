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
import com.google.zxing.common.BitArrayUtils;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author mysen@google.com (Chris Mysen) - ported from C++
 */
public final class MatrixUtilTestCase extends Assert {

  @Test
  public void testToString() {
    ByteMatrix array = new ByteMatrix(3, 3);
    array.set(0, 0, 0);
    array.set(1, 0, 1);
    array.set(2, 0, 0);
    array.set(0, 1, 1);
    array.set(1, 1, 0);
    array.set(2, 1, 1);
    array.set(0, 2, -1);
    array.set(1, 2, -1);
    array.set(2, 2, -1);
    String expected = " 0 1 0\n" + " 1 0 1\n" + "      \n";
    assertEquals(expected, toString(array));
  }

  @Test
  public void testClearMatrix() {
    ByteMatrix matrix = new ByteMatrix(2, 2);
    MatrixUtil.clearMatrix(matrix);
    assertEquals(-1, matrix.get(0, 0));
    assertEquals(-1, matrix.get(1, 0));
    assertEquals(-1, matrix.get(0, 1));
    assertEquals(-1, matrix.get(1, 1));
  }

  @Test
  public void testEmbedBasicPatterns1() throws WriterException {
    // Version 1.
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.clearMatrix(matrix);
    MatrixUtil.embedBasicPatterns(1, matrix);
    String expected =
        " 1 1 1 1 1 1 1 0           0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0           0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0           0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0           0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0           0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0           0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0           0 0 0 0 0 0 0 0\n" +
        "             1                            \n" +
        "             0                            \n" +
        "             1                            \n" +
        "             0                            \n" +
        "             1                            \n" +
        " 0 0 0 0 0 0 0 0 1                        \n" +
        " 1 1 1 1 1 1 1 0                          \n" +
        " 1 0 0 0 0 0 1 0                          \n" +
        " 1 0 1 1 1 0 1 0                          \n" +
        " 1 0 1 1 1 0 1 0                          \n" +
        " 1 0 1 1 1 0 1 0                          \n" +
        " 1 0 0 0 0 0 1 0                          \n" +
        " 1 1 1 1 1 1 1 0                          \n";
    assertEquals(expected, toString(matrix));
  }

  @Test
  public void testEmbedBasicPatterns2() throws WriterException {
    // Version 2.  Position adjustment pattern should apppear at right
    // bottom corner.
    ByteMatrix matrix = new ByteMatrix(25, 25);
    MatrixUtil.clearMatrix(matrix);
    MatrixUtil.embedBasicPatterns(2, matrix);
    String expected =
        " 1 1 1 1 1 1 1 0                   0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0                   0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0                   0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0                   0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0                   0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0                   0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0                   0 0 0 0 0 0 0 0\n" +
        "             1                                    \n" +
        "             0                                    \n" +
        "             1                                    \n" +
        "             0                                    \n" +
        "             1                                    \n" +
        "             0                                    \n" +
        "             1                                    \n" +
        "             0                                    \n" +
        "             1                   1 1 1 1 1        \n" +
        " 0 0 0 0 0 0 0 0 1               1 0 0 0 1        \n" +
        " 1 1 1 1 1 1 1 0                 1 0 1 0 1        \n" +
        " 1 0 0 0 0 0 1 0                 1 0 0 0 1        \n" +
        " 1 0 1 1 1 0 1 0                 1 1 1 1 1        \n" +
        " 1 0 1 1 1 0 1 0                                  \n" +
        " 1 0 1 1 1 0 1 0                                  \n" +
        " 1 0 0 0 0 0 1 0                                  \n" +
        " 1 1 1 1 1 1 1 0                                  \n";
    assertEquals(expected, toString(matrix));
  }

  @Test
  public void testEmbedTypeInfo() throws WriterException {
    // Type info bits = 100000011001110.
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.clearMatrix(matrix);
    MatrixUtil.embedTypeInfo(ErrorCorrectionLevel.M, 5, matrix);
    String expected =
        "                 0                        \n" +
        "                 1                        \n" +
        "                 1                        \n" +
        "                 1                        \n" +
        "                 0                        \n" +
        "                 0                        \n" +
        "                                          \n" +
        "                 1                        \n" +
        " 1 0 0 0 0 0   0 1         1 1 0 0 1 1 1 0\n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                 0                        \n" +
        "                 0                        \n" +
        "                 0                        \n" +
        "                 0                        \n" +
        "                 0                        \n" +
        "                 0                        \n" +
        "                 1                        \n";
    assertEquals(expected, toString(matrix));
  }

  @Test
  public void testEmbedVersionInfo() throws WriterException {
    // Version info bits = 000111 110010 010100
    // Actually, version 7 QR Code has 45x45 matrix but we use 21x21 here
    // since 45x45 matrix is too big to depict.
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.clearMatrix(matrix);
    MatrixUtil.maybeEmbedVersionInfo(7, matrix);
    String expected =
        "                     0 0 1                \n" +
        "                     0 1 0                \n" +
        "                     0 1 0                \n" +
        "                     0 1 1                \n" +
        "                     1 1 1                \n" +
        "                     0 0 0                \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        " 0 0 0 0 1 0                              \n" +
        " 0 1 1 1 1 0                              \n" +
        " 1 0 0 1 1 0                              \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n" +
        "                                          \n";
    assertEquals(expected, toString(matrix));
  }

  @Test
  public void testEmbedDataBits() throws WriterException {
    // Cells other than basic patterns should be filled with zero.
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.clearMatrix(matrix);
    MatrixUtil.embedBasicPatterns(1, matrix);
    BitArray bits = new BitArray();
    MatrixUtil.embedDataBits(bits, -1, matrix);
    String expected =
        " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n";
    assertEquals(expected, toString(matrix));
  }

  @Test
  public void testBuildMatrix() throws WriterException {
    // From http://www.swetake.com/qr/qr7.html
    char[] bytes = {32, 65, 205, 69, 41, 220, 46, 128, 236,
        42, 159, 74, 221, 244, 169, 239, 150, 138,
        70, 237, 85, 224, 96, 74, 219 , 61};
    BitArray bits = new BitArray();
    for (char c: bytes) {
      bits.appendBits(c, 8);
    }
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.buildMatrix(bits,
                           ErrorCorrectionLevel.H,
                           1,  // Version 1
                           3,  // Mask pattern 3
                           matrix);
    String expected =
        " 1 1 1 1 1 1 1 0 0 1 1 0 0 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 0 0 1 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 0 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 0 1 1 1 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 1 1 0 1 1 0 0 0 0 0 0 0 0\n" +
        " 0 0 1 1 0 0 1 1 1 0 0 1 1 1 1 0 1 0 0 0 0\n" +
        " 1 0 1 0 1 0 0 0 0 0 1 1 1 0 0 1 0 1 1 1 0\n" +
        " 1 1 1 1 0 1 1 0 1 0 1 1 1 0 0 1 1 1 0 1 0\n" +
        " 1 0 1 0 1 1 0 1 1 1 0 0 1 1 1 0 0 1 0 1 0\n" +
        " 0 0 1 0 0 1 1 1 0 0 0 0 0 0 1 0 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 1 1 0 1 0 0 0 0 0 1 0 1 1\n" +
        " 1 1 1 1 1 1 1 0 1 1 1 1 0 0 0 0 1 0 1 1 0\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 1 0 1 1 1 0 0 0 0 0\n" +
        " 1 0 1 1 1 0 1 0 0 1 0 0 1 1 0 0 1 0 0 1 1\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 1 0 0 0 0 0 1 1 1 0\n" +
        " 1 0 1 1 1 0 1 0 1 1 1 1 0 0 0 0 1 1 1 0 0\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0 1 0 0\n" +
        " 1 1 1 1 1 1 1 0 0 0 1 1 1 1 1 0 1 0 0 1 0\n";
    assertEquals(expected, toString(matrix));
  }

  @Test
  public void testFindMSBSet() {
    assertEquals(0, 32 - Integer.numberOfLeadingZeros(0));
    assertEquals(1, 32 - Integer.numberOfLeadingZeros(1));
    assertEquals(8, 32 - Integer.numberOfLeadingZeros(0x80));
    assertEquals(32, 32 - Integer.numberOfLeadingZeros(0x80000000));
  }

  @Test
  public void testCalculateBCHCode() {
    // Encoding of type information.
    // From Appendix C in JISX0510:2004 (p 65)
    assertEquals(0xdc, MatrixUtil.calculateBCHCode(5, 0x537));
    // From http://www.swetake.com/qr/qr6.html
    assertEquals(0x1c2, MatrixUtil.calculateBCHCode(0x13, 0x537));
    // From http://www.swetake.com/qr/qr11.html
    assertEquals(0x214, MatrixUtil.calculateBCHCode(0x1b, 0x537));

    // Encoding of version information.
    // From Appendix D in JISX0510:2004 (p 68)
    assertEquals(0xc94, MatrixUtil.calculateBCHCode(7, 0x1f25));
    assertEquals(0x5bc, MatrixUtil.calculateBCHCode(8, 0x1f25));
    assertEquals(0xa99, MatrixUtil.calculateBCHCode(9, 0x1f25));
    assertEquals(0x4d3, MatrixUtil.calculateBCHCode(10, 0x1f25));
    assertEquals(0x9a6, MatrixUtil.calculateBCHCode(20, 0x1f25));
    assertEquals(0xd75, MatrixUtil.calculateBCHCode(30, 0x1f25));
    assertEquals(0xc69, MatrixUtil.calculateBCHCode(40, 0x1f25));
  }

  // We don't test a lot of cases in this function since we've already
  // tested them in TEST(calculateBCHCode).
  @Test
  public void testMakeVersionInfoBits() throws WriterException {
    // From Appendix D in JISX0510:2004 (p 68)
    BitArray bits = new BitArray();
    MatrixUtil.makeVersionInfoBits(7, bits);
    assertEquals(" ...XXXXX ..X..X.X ..", BitArrayUtils.toString(bits));
  }

  // We don't test a lot of cases in this function since we've already
  // tested them in TEST(calculateBCHCode).
  @Test
  public void testMakeTypeInfoInfoBits() throws WriterException {
    // From Appendix C in JISX0510:2004 (p 65)
    BitArray bits = new BitArray();
    MatrixUtil.makeTypeInfoBits(ErrorCorrectionLevel.M, 5, bits);
    assertEquals(" X......X X..XXX.", BitArrayUtils.toString(bits));
  }

  public static String toString(ByteMatrix matrix) {
    int width = matrix.width, height = matrix.height;
    StringBuilder result = new StringBuilder(2 * width * height + 2);
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        result.append(' '); // Mike-CHANGED: using accessor, appending chars
        switch (matrix.get(x, y)) {
          case 0:
            result.append('0');
            break;
          case 1:
            result.append('1');
            break;
          default:
            result.append(' ');
            break;
        }
      }
      result.append('\n');
    }
    return result.toString();
  }
}
