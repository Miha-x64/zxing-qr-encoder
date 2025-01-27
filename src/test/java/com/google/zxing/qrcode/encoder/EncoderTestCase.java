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
import com.google.zxing.common.BitArrayUtils;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;

import org.junit.Assert;
import org.junit.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author mysen@google.com (Chris Mysen) - ported from C++
 */
public final class EncoderTestCase extends Assert {

  @Test
  public void testGetAlphanumericCode() {
    // The first ten code points are numbers.
    for (int i = 0; i < 10; ++i) {
      assertEquals(i, Encoder.getAlphanumericCode('0' + i));
    }

    // The next 26 code points are capital alphabet letters.
    for (int i = 10; i < 36; ++i) {
      assertEquals(i, Encoder.getAlphanumericCode('A' + i - 10));
    }

    // Others are symbol letters
    assertEquals(36, Encoder.getAlphanumericCode(' '));
    assertEquals(37, Encoder.getAlphanumericCode('$'));
    assertEquals(38, Encoder.getAlphanumericCode('%'));
    assertEquals(39, Encoder.getAlphanumericCode('*'));
    assertEquals(40, Encoder.getAlphanumericCode('+'));
    assertEquals(41, Encoder.getAlphanumericCode('-'));
    assertEquals(42, Encoder.getAlphanumericCode('.'));
    assertEquals(43, Encoder.getAlphanumericCode('/'));
    assertEquals(44, Encoder.getAlphanumericCode(':'));

    // Should return -1 for other letters;
    assertEquals(-1, Encoder.getAlphanumericCode('a'));
    assertEquals(-1, Encoder.getAlphanumericCode('#'));
    assertEquals(-1, Encoder.getAlphanumericCode('\0'));
  }

  @Test
  public void testChooseMode() {
    // Numeric mode.
    assertSame(Mode.NUMERIC, Encoder_chooseMode("0"));
    assertSame(Mode.NUMERIC, Encoder_chooseMode("0123456789"));
    // Alphanumeric mode.
    assertSame(Mode.ALPHANUMERIC, Encoder_chooseMode("A"));
    assertSame(Mode.ALPHANUMERIC, Encoder_chooseMode("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"));
    // 8-bit byte mode.
    assertSame(Mode.BYTE, Encoder_chooseMode("a"));
    assertSame(Mode.BYTE, Encoder_chooseMode("#"));
    assertSame(Mode.BYTE, Encoder_chooseMode(""));
    // Kanji mode.  We used to use MODE_KANJI for these, but we stopped
    // doing that as we cannot distinguish Shift_JIS from other encodings
    // from data bytes alone.  See also comments in qrcode_encoder.h.

    // AIUE in Hiragana in Shift_JIS
    assertSame(Mode.BYTE, Encoder_chooseMode(shiftJISString(bytes(0x8, 0xa, 0x8, 0xa, 0x8, 0xa, 0x8, 0xa6))));

    // Nihon in Kanji in Shift_JIS.
    assertSame(Mode.BYTE, Encoder_chooseMode(shiftJISString(bytes(0x9, 0xf, 0x9, 0x7b))));

    // Sou-Utsu-Byou in Kanji in Shift_JIS.
    assertSame(Mode.BYTE, Encoder_chooseMode(shiftJISString(bytes(0xe, 0x4, 0x9, 0x5, 0x9, 0x61))));
  }

  private static Mode Encoder_chooseMode(String a) {
    return Encoder.chooseMode(a, null);
  }

  @Test
  public void testEncode() throws WriterException {
    QRCode qrCode = Encoder.encode("ABCDEF", ErrorCorrectionLevel.H);
    String expected = "<<\n" +
        " mode: ALPHANUMERIC\n" +
        " ecLevel: H\n" +
        " version: 1\n" +
        " maskPattern: 0\n" +
        " matrix:\n" +
        " 1 1 1 1 1 1 1 0 1 1 1 1 0 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 1 1 0 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 0 1 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 1 1 0 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 1 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 0 0 0 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 0 0 1 0 1 0 0 0 0 0 0 0 0\n" +
        " 0 0 1 0 1 1 1 0 1 1 0 0 1 1 0 0 0 1 0 0 1\n" +
        " 1 0 1 1 1 0 0 1 0 0 0 1 0 1 0 0 0 0 0 0 0\n" +
        " 0 0 1 1 0 0 1 0 1 0 0 0 1 0 1 0 1 0 1 1 0\n" +
        " 1 1 0 1 0 1 0 1 1 1 0 1 0 1 0 0 0 0 0 1 0\n" +
        " 0 0 1 1 0 1 1 1 1 0 0 0 1 0 1 0 1 1 1 1 0\n" +
        " 0 0 0 0 0 0 0 0 1 0 0 1 1 1 0 1 0 1 0 0 0\n" +
        " 1 1 1 1 1 1 1 0 0 0 1 0 1 0 1 1 0 0 0 0 1\n" +
        " 1 0 0 0 0 0 1 0 1 1 1 1 0 1 0 1 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 1 0 1 0 1 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 0 1 1 1 1 0 1 0 1 0\n" +
        " 1 0 1 1 1 0 1 0 1 0 0 0 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 1 0 1 1 0 1 0 0 0 1 1\n" +
        " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 1 0 1 0 1\n" +
        ">>\n";
    assertEquals(expected, QRCodeTestCase.toString(qrCode));
  }

  @Test
  public void testEncodeWithVersion() throws WriterException {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.QR_VERSION, 7);
    QRCode qrCode = Encoder.encode("ABCDEF", ErrorCorrectionLevel.H, hints);
    assertTrue(QRCodeTestCase.toString(qrCode).contains(" version: 7\n"));
  }

  @Test(expected = WriterException.class)
  public void testEncodeWithVersionTooSmall() throws WriterException {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.QR_VERSION, 3);
    Encoder.encode("THISMESSAGEISTOOLONGFORAQRCODEVERSION3", ErrorCorrectionLevel.H, hints);
  }

  @Test
  public void testSimpleUTF8ECI() throws WriterException {
    Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF8");
    QRCode qrCode = Encoder.encode("hello", ErrorCorrectionLevel.H, hints);
    String expected = "<<\n" +
        " mode: BYTE\n" +
        " ecLevel: H\n" +
        " version: 1\n" +
        " maskPattern: 3\n" +
        " matrix:\n" +
        " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 0 1 0 1 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 0 1 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 0 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 0 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 0 1 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0\n" +
        " 0 0 1 1 0 0 1 1 1 1 0 0 0 1 1 0 1 0 0 0 0\n" +
        " 0 0 1 1 1 0 0 0 0 0 1 1 0 0 0 1 0 1 1 1 0\n" +
        " 0 1 0 1 0 1 1 1 0 1 0 1 0 0 0 0 0 1 1 1 1\n" +
        " 1 1 0 0 1 0 0 1 1 0 0 1 1 1 1 0 1 0 1 1 0\n" +
        " 0 0 0 0 1 0 1 1 1 1 0 0 0 0 0 1 0 0 1 0 0\n" +
        " 0 0 0 0 0 0 0 0 1 1 1 1 0 0 1 1 1 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 1 1 0 1 0 1 1 0 0 1 0 0\n" +
        " 1 0 0 0 0 0 1 0 0 0 1 0 0 1 1 1 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 0 0 0 0 1 1 0 0 0 0 0\n" +
        " 1 0 1 1 1 0 1 0 1 1 1 0 1 0 0 0 1 1 0 0 0\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 0 0 1 0 0 1 0 0 0 0\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 1 1 0 1 0 1 0 1 1 0\n" +
        " 1 1 1 1 1 1 1 0 0 1 0 1 1 1 0 1 1 0 0 0 0\n" +
        ">>\n";
    assertEquals(expected, QRCodeTestCase.toString(qrCode));
  }

  @Test
  public void testEncodeKanjiMode() throws WriterException {
    Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, "Shift_JIS");
    // Nihon in Kanji
    QRCode qrCode = Encoder.encode("\u65e5\u672c", ErrorCorrectionLevel.M, hints);
    String expected = "<<\n" +
        " mode: KANJI\n" +
        " ecLevel: M\n" +
        " version: 1\n" +
        " maskPattern: 4\n" +
        " matrix:\n" +
        " 1 1 1 1 1 1 1 0 1 1 1 1 0 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 1 1 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 0 1 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 0 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 1 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 1 0 1 0 1 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 0 0 0 1 0 1 1 1 0 0 0 1 1 1 1 1 1 0 0 1\n" +
        " 0 1 1 0 0 1 0 1 1 0 1 0 1 1 1 0 0 0 1 0 1\n" +
        " 1 1 1 1 0 1 1 1 0 0 1 0 1 1 0 0 0 0 1 1 1\n" +
        " 1 0 1 0 1 1 0 0 0 0 1 1 1 0 0 1 0 0 1 1 0\n" +
        " 0 0 1 0 1 1 1 1 1 1 1 1 0 0 1 1 1 1 0 1 1\n" +
        " 0 0 0 0 0 0 0 0 1 1 1 1 1 0 0 1 0 1 0 0 0\n" +
        " 1 1 1 1 1 1 1 0 1 1 0 1 0 0 1 1 1 1 1 1 0\n" +
        " 1 0 0 0 0 0 1 0 0 0 0 0 0 1 1 0 1 0 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 0 1 1 1 0 0 0 1 1 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 0 0 1 1 1 0 0 0 1 1 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 0 1 1 0 0 0 1 0 0 0\n" +
        " 1 0 0 0 0 0 1 0 0 0 1 1 1 0 0 1 0 1 0 0 0\n" +
        " 1 1 1 1 1 1 1 0 1 1 1 1 0 0 1 1 1 0 1 1 0\n" +
        ">>\n";
    assertEquals(expected, QRCodeTestCase.toString(qrCode));
  }

  @Test
  public void testEncodeShiftjisNumeric() throws WriterException {
    Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, "Shift_JIS");
    QRCode qrCode = Encoder.encode("0123", ErrorCorrectionLevel.M, hints);
    String expected = "<<\n" +
        " mode: NUMERIC\n" +
        " ecLevel: M\n" +
        " version: 1\n" +
        " maskPattern: 0\n" +
        " matrix:\n" +
        " 1 1 1 1 1 1 1 0 0 0 0 0 1 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 1 1 0 1 0 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 0 1 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 1 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 0 1 0 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0\n" +
        " 1 0 1 0 1 0 1 0 0 0 0 0 1 0 0 0 1 0 0 1 0\n" +
        " 0 0 0 0 0 0 0 1 1 0 1 1 0 1 0 1 0 1 0 1 0\n" +
        " 0 1 0 1 0 1 1 1 1 0 0 1 0 1 1 1 0 1 0 1 0\n" +
        " 0 1 1 1 0 0 0 0 0 0 1 1 1 1 0 1 1 1 0 1 0\n" +
        " 0 0 0 1 1 1 1 1 1 1 1 1 0 1 1 1 0 0 1 0 1\n" +
        " 0 0 0 0 0 0 0 0 1 1 0 0 0 0 1 0 0 0 1 1 0\n" +
        " 1 1 1 1 1 1 1 0 0 1 0 0 1 0 0 0 1 0 0 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 0 0 0 0 1 0 0 0 1 0 0\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 0 1 0 1 0 1 0 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 1 0 1 1 1 0 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 0 1 1 1 1 0 1 1 1 0 0 0\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 1 0 1 1 1 0 1 1 0 1\n" +
        ">>\n";
    assertEquals(expected, QRCodeTestCase.toString(qrCode));
  }

  @Test
  public void testEncodeGS1WithStringTypeHint() throws WriterException {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.GS1_FORMAT, "true");
    QRCode qrCode = Encoder.encode("100001%11171218", ErrorCorrectionLevel.H, hints);
    verifyGS1EncodedData(qrCode);
  }

  @Test
  public void testEncodeGS1WithBooleanTypeHint() throws WriterException {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.GS1_FORMAT, true);
    QRCode qrCode = Encoder.encode("100001%11171218", ErrorCorrectionLevel.H, hints);
    verifyGS1EncodedData(qrCode);
  }

  @Test
  public void testDoesNotEncodeGS1WhenBooleanTypeHintExplicitlyFalse() throws WriterException {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.GS1_FORMAT, false);
    QRCode qrCode = Encoder.encode("ABCDEF", ErrorCorrectionLevel.H, hints);
    verifyNotGS1EncodedData(qrCode);
  }

  @Test
  public void testDoesNotEncodeGS1WhenStringTypeHintExplicitlyFalse() throws WriterException {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.GS1_FORMAT, "false");
    QRCode qrCode = Encoder.encode("ABCDEF", ErrorCorrectionLevel.H, hints);
    verifyNotGS1EncodedData(qrCode);
  }

  @Test
  public void testGS1ModeHeaderWithECI() throws WriterException {
    Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF8");
    hints.put(EncodeHintType.GS1_FORMAT, true);
    QRCode qrCode = Encoder.encode("hello", ErrorCorrectionLevel.H, hints);
    String expected = "<<\n" +
        " mode: BYTE\n" +
        " ecLevel: H\n" +
        " version: 1\n" +
        " maskPattern: 6\n" +
        " matrix:\n" +
        " 1 1 1 1 1 1 1 0 0 0 1 1 0 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 1 0 0 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 1 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 0 1 1 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 0 0 1 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0\n" +
        " 0 0 0 1 1 0 1 1 0 1 0 0 0 0 0 0 0 1 1 0 0\n" +
        " 0 1 0 1 1 0 0 1 0 1 1 1 1 1 1 0 1 1 1 0 1\n" +
        " 0 1 1 1 1 0 1 0 0 1 0 1 0 1 1 1 0 0 1 0 1\n" +
        " 1 1 1 1 1 0 0 1 0 0 0 1 1 0 0 1 0 0 1 0 0\n" +
        " 1 0 0 1 0 0 1 1 0 1 1 0 1 0 1 0 0 1 0 0 1\n" +
        " 0 0 0 0 0 0 0 0 1 1 1 1 1 1 0 0 1 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 0 1 0 1 1 0 1 0 1 0 0\n" +
        " 1 0 0 0 0 0 1 0 0 1 0 0 0 0 1 0 1 1 1 0 0\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 1 1 0 0 0 1 1 0 0 0\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 1 1 1 1 0 0 0 1 1 0\n" +
        " 1 0 1 1 1 0 1 0 0 0 1 0 0 1 0 0 1 0 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 0 0 0 0 0 0 0 1 1 0 0\n" +
        " 1 1 1 1 1 1 1 0 0 1 0 1 0 0 1 0 0 0 0 0 0\n" +
        ">>\n";
    assertEquals(expected, QRCodeTestCase.toString(qrCode));
  }

  @Test
  public void testAppendModeInfo() {
    BitArray bits = new BitArray();
    bits.appendBits(Mode.NUMERIC.getBits(), 4);
    assertEquals(" ...X", BitArrayUtils.toString(bits));
  }

  @Test
  public void testAppendLengthInfo() throws WriterException {
    BitArray bits = new BitArray();
    Encoder.appendLengthInfo(1,  // 1 letter (1/1).
                             1,
                             Mode.NUMERIC,
                             bits);
    assertEquals(" ........ .X", BitArrayUtils.toString(bits));  // 10 bits.
    bits = new BitArray();
    Encoder.appendLengthInfo(2,  // 2 letters (2/1).
                             10,
                             Mode.ALPHANUMERIC,
                             bits);
    assertEquals(" ........ .X.", BitArrayUtils.toString(bits));  // 11 bits.
    bits = new BitArray();
    Encoder.appendLengthInfo(255,  // 255 letter (255/1).
                             27,
                             Mode.BYTE,
                             bits);
    assertEquals(" ........ XXXXXXXX", BitArrayUtils.toString(bits));  // 16 bits.
    bits = new BitArray();
    Encoder.appendLengthInfo(512,  // 512 letters (1024/2).
                             40,
                             Mode.KANJI,
                             bits);
    assertEquals(" ..X..... ....", BitArrayUtils.toString(bits));  // 12 bits.
  }

  private static void Encoder_appendBytes(String content, Mode mode, BitArray bits) throws WriterException {
    Encoder.appendBytes(content, 0, content.length(), mode, bits, Encoder.DEFAULT_BYTE_MODE_ENCODING);
  }
  @Test
  public void testAppendBytes() throws WriterException {
    // Should use appendNumericBytes.
    // 1 = 01 = 0001 in 4 bits.
    BitArray bits = new BitArray();
    Encoder_appendBytes("1", Mode.NUMERIC, bits);
    assertEquals(" ...X" , BitArrayUtils.toString(bits));
    // Should use appendAlphanumericBytes.
    // A = 10 = 0xa = 001010 in 6 bits
    bits = new BitArray();
    Encoder_appendBytes("A", Mode.ALPHANUMERIC, bits);
    assertEquals(" ..X.X." , BitArrayUtils.toString(bits));
    // Lower letters such as 'a' cannot be encoded in MODE_ALPHANUMERIC.
    try {
      Encoder_appendBytes("a", Mode.ALPHANUMERIC, bits);
    } catch (WriterException we) {
      // good
    }
    // Should use append8BitBytes.
    // 0x61, 0x62, 0x63
    bits = new BitArray();
    Encoder_appendBytes("abc", Mode.BYTE, bits);
    assertEquals(" .XX....X .XX...X. .XX...XX", BitArrayUtils.toString(bits));
    // Anything can be encoded in QRCode.MODE_8BIT_BYTE.
    Encoder_appendBytes("\0", Mode.BYTE, bits);
    // Should use appendKanjiBytes.
    // 0x93, 0x5f
    bits = new BitArray();
    Encoder_appendBytes(shiftJISString(bytes(0x93, 0x5f)), Mode.KANJI, bits);
    assertEquals(" .XX.XX.. XXXXX", BitArrayUtils.toString(bits));
  }

  @Test
  public void testTerminateBits() throws WriterException {
    BitArray v = new BitArray();
    Encoder.terminateBits(0, v);
    assertEquals("", BitArrayUtils.toString(v));
    v = new BitArray();
    Encoder.terminateBits(1, v);
    assertEquals(" ........", BitArrayUtils.toString(v));
    v = new BitArray();
    v.appendBits(0, 3);  // Append 000
    Encoder.terminateBits(1, v);
    assertEquals(" ........", BitArrayUtils.toString(v));
    v = new BitArray();
    v.appendBits(0, 5);  // Append 00000
    Encoder.terminateBits(1, v);
    assertEquals(" ........", BitArrayUtils.toString(v));
    v = new BitArray();
    v.appendBits(0, 8);  // Append 00000000
    Encoder.terminateBits(1, v);
    assertEquals(" ........", BitArrayUtils.toString(v));
    v = new BitArray();
    Encoder.terminateBits(2, v);
    assertEquals(" ........ XXX.XX..", BitArrayUtils.toString(v));
    v = new BitArray();
    v.appendBits(0, 1);  // Append 0
    Encoder.terminateBits(3, v);
    assertEquals(" ........ XXX.XX.. ...X...X", BitArrayUtils.toString(v));
  }

  @Test
  public void testGetNumDataBytesAndNumECBytesForBlockID() throws WriterException {
    // Version 1-H.
    long dataAndEcBytesCount = Encoder.getNumDataBytesAndNumECBytesForBlockID(26, 9, 1, 0);
    assertEquals(9L << 32 | 17 , dataAndEcBytesCount);

    // Version 3-H.  2 blocks.
    dataAndEcBytesCount = Encoder.getNumDataBytesAndNumECBytesForBlockID(70, 26, 2, 0);
    assertEquals(13L << 32 | 22, dataAndEcBytesCount);
    dataAndEcBytesCount = Encoder.getNumDataBytesAndNumECBytesForBlockID(70, 26, 2, 1);
    assertEquals(13L << 32 | 22, dataAndEcBytesCount);

    // Version 7-H. (4 + 1) blocks.
    dataAndEcBytesCount = Encoder.getNumDataBytesAndNumECBytesForBlockID(196, 66, 5, 0);
    assertEquals(13L << 32 | 26, dataAndEcBytesCount);
    dataAndEcBytesCount = Encoder.getNumDataBytesAndNumECBytesForBlockID(196, 66, 5, 4);
    assertEquals(14L << 32 | 26, dataAndEcBytesCount);

    // Version 40-H. (20 + 61) blocks.
    dataAndEcBytesCount = Encoder.getNumDataBytesAndNumECBytesForBlockID(3706, 1276, 81, 0);
    assertEquals(15L << 32 | 30, dataAndEcBytesCount);
    dataAndEcBytesCount = Encoder.getNumDataBytesAndNumECBytesForBlockID(3706, 1276, 81, 20);
    assertEquals(16L << 32 | 30, dataAndEcBytesCount);
    dataAndEcBytesCount = Encoder.getNumDataBytesAndNumECBytesForBlockID(3706, 1276, 81, 80);
    assertEquals(16L << 32 | 30, dataAndEcBytesCount);
  }

  @Test
  public void testInterleaveWithECBytes() throws WriterException {
    byte[] dataBytes = bytes(32, 65, 205, 69, 41, 220, 46, 128, 236);
    BitArray in = new BitArray();
    for (byte dataByte: dataBytes) {
      in.appendBits(dataByte, 8);
    }
    BitArray out = Encoder.interleaveWithECBytes(in, 26, 9, 1);
    byte[] expected = bytes(
        // Data bytes.
        32, 65, 205, 69, 41, 220, 46, 128, 236,
        // Error correction bytes.
        42, 159, 74, 221, 244, 169, 239, 150, 138, 70,
        237, 85, 224, 96, 74, 219, 61
    );
    assertEquals(expected.length, out.getSizeInBytes());
    byte[] outArray = new byte[expected.length];
    out.toBytes(0, outArray, 0, expected.length);
    // Can't use Arrays.equals(), because outArray may be longer than out.sizeInBytes()
    for (int x = 0; x < expected.length; x++) {
      assertEquals(expected[x], outArray[x]);
    }
    // Numbers are from http://www.swetake.com/qr/qr8.html
    dataBytes = bytes(
        67, 70, 22, 38, 54, 70, 86, 102, 118, 134, 150, 166, 182,
        198, 214, 230, 247, 7, 23, 39, 55, 71, 87, 103, 119, 135,
        151, 166, 22, 38, 54, 70, 86, 102, 118, 134, 150, 166,
        182, 198, 214, 230, 247, 7, 23, 39, 55, 71, 87, 103, 119,
        135, 151, 160, 236, 17, 236, 17, 236, 17, 236,
        17
    );
    in = new BitArray();
    for (byte dataByte: dataBytes) {
      in.appendBits(dataByte, 8);
    }

    out = Encoder.interleaveWithECBytes(in, 134, 62, 4);
    expected = bytes(
        // Data bytes.
        67, 230, 54, 55, 70, 247, 70, 71, 22, 7, 86, 87, 38, 23, 102, 103, 54, 39,
        118, 119, 70, 55, 134, 135, 86, 71, 150, 151, 102, 87, 166,
        160, 118, 103, 182, 236, 134, 119, 198, 17, 150,
        135, 214, 236, 166, 151, 230, 17, 182,
        166, 247, 236, 198, 22, 7, 17, 214, 38, 23, 236, 39,
        17,
        // Error correction bytes.
        175, 155, 245, 236, 80, 146, 56, 74, 155, 165,
        133, 142, 64, 183, 132, 13, 178, 54, 132, 108, 45,
        113, 53, 50, 214, 98, 193, 152, 233, 147, 50, 71, 65,
        190, 82, 51, 209, 199, 171, 54, 12, 112, 57, 113, 155, 117,
        211, 164, 117, 30, 158, 225, 31, 190, 242, 38,
        140, 61, 179, 154, 214, 138, 147, 87, 27, 96, 77, 47,
        187, 49, 156, 214
    );
    assertEquals(expected.length, out.getSizeInBytes());
    outArray = new byte[expected.length];
    out.toBytes(0, outArray, 0, expected.length);
    for (int x = 0; x < expected.length; x++) {
      assertEquals(expected[x], outArray[x]);
    }
  }

  private static byte[] bytes(int... ints) {
    byte[] bytes = new byte[ints.length];
    for (int i = 0; i < ints.length; i++) {
      bytes[i] = (byte) ints[i];
    }
    return bytes;
  }

  @Test
  public void testAppendNumericBytes() {
    // 1 = 01 = 0001 in 4 bits.
    BitArray bits = new BitArray();
    Encoder.appendNumericBytes("1", bits, 0, 1);
    assertEquals(" ...X" , BitArrayUtils.toString(bits));
    // 12 = 0xc = 0001100 in 7 bits.
    bits = new BitArray();
    Encoder.appendNumericBytes("12", bits, 0, 2);
    assertEquals(" ...XX.." , BitArrayUtils.toString(bits));
    // 123 = 0x7b = 0001111011 in 10 bits.
    bits = new BitArray();
    Encoder.appendNumericBytes("123", bits, 0, 3);
    assertEquals(" ...XXXX. XX" , BitArrayUtils.toString(bits));
    // 1234 = "123" + "4" = 0001111011 + 0100
    bits = new BitArray();
    Encoder.appendNumericBytes("1234", bits, 0, 4);
    assertEquals(" ...XXXX. XX.X.." , BitArrayUtils.toString(bits));
    // Empty.
    bits = new BitArray();
    Encoder.appendNumericBytes("", bits, 0, 0);
    assertEquals("" , BitArrayUtils.toString(bits));
  }

  @Test
  public void testAppendAlphanumericBytes() throws WriterException {
    // A = 10 = 0xa = 001010 in 6 bits
    BitArray bits = new BitArray();
    Encoder.appendAlphanumericBytes("A", bits, 0, 1);
    assertEquals(" ..X.X." , BitArrayUtils.toString(bits));
    // AB = 10 * 45 + 11 = 461 = 0x1cd = 00111001101 in 11 bits
    bits = new BitArray();
    Encoder.appendAlphanumericBytes("AB", bits, 0, 2);
    assertEquals(" ..XXX..X X.X", BitArrayUtils.toString(bits));
    // ABC = "AB" + "C" = 00111001101 + 001100
    bits = new BitArray();
    Encoder.appendAlphanumericBytes("ABC", bits, 0, 3);
    assertEquals(" ..XXX..X X.X..XX. ." , BitArrayUtils.toString(bits));
    // Empty.
    bits = new BitArray();
    Encoder.appendAlphanumericBytes("", bits, 0, 0);
    assertEquals("" , BitArrayUtils.toString(bits));
    // Invalid data.
    try {
      Encoder.appendAlphanumericBytes("abc", new BitArray(), 0, 3);
    } catch (WriterException we) {
      // good
    }
  }

  @Test
  public void testAppend8BitBytes() {
    // 0x61, 0x62, 0x63
    BitArray bits = new BitArray();
    Encoder.append8BitBytes("abc", bits, Encoder.DEFAULT_BYTE_MODE_ENCODING);
    assertEquals(" .XX....X .XX...X. .XX...XX", BitArrayUtils.toString(bits));
    // Empty.
    bits = new BitArray();
    Encoder.append8BitBytes("", bits, Encoder.DEFAULT_BYTE_MODE_ENCODING);
    assertEquals("", BitArrayUtils.toString(bits));
  }

  // Numbers are from page 21 of JISX0510:2004
  @Test
  public void testAppendKanjiBytes() throws WriterException {
    BitArray bits = new BitArray();
    Encoder.appendKanjiBytes(shiftJISString(bytes(0x93, 0x5f)), bits);
    assertEquals(" .XX.XX.. XXXXX", BitArrayUtils.toString(bits));
    Encoder.appendKanjiBytes(shiftJISString(bytes(0xe4, 0xaa)), bits);
    assertEquals(" .XX.XX.. XXXXXXX. X.X.X.X. X.", BitArrayUtils.toString(bits));
  }

  // Numbers are from http://www.swetake.com/qr/qr3.html and
  // http://www.swetake.com/qr/qr9.html
  @Test
  public void testGenerateECBytes() {
    byte[] dataBytes = bytes(32, 65, 205, 69, 41, 220, 46, 128, 236);
    byte[] ecBytes = Encoder.generateECBytes(dataBytes, 17);
    int[] expected = {
        42, 159, 74, 221, 244, 169, 239, 150, 138, 70, 237, 85, 224, 96, 74, 219, 61
    };
    assertEquals(expected.length, ecBytes.length);
    for (int x = 0; x < expected.length; x++) {
      assertEquals(expected[x], ecBytes[x] & 0xFF);
    }
    dataBytes = bytes(67, 70, 22, 38, 54, 70, 86, 102, 118, 134, 150, 166,  182, 198, 214);
    ecBytes = Encoder.generateECBytes(dataBytes, 18);
    expected = new int[] {
        175, 80, 155, 64, 178, 45, 214, 233, 65, 209, 12, 155, 117, 31, 140, 214, 27, 187
    };
    assertEquals(expected.length, ecBytes.length);
    for (int x = 0; x < expected.length; x++) {
      assertEquals(expected[x], ecBytes[x] & 0xFF);
    }
    // High-order zero coefficient case.
    dataBytes = bytes(32, 49, 205, 69, 42, 20, 0, 236, 17);
    ecBytes = Encoder.generateECBytes(dataBytes, 17);
    expected = new int[] {
        0, 3, 130, 179, 194, 0, 55, 211, 110, 79, 98, 72, 170, 96, 211, 137, 213
    };
    assertEquals(expected.length, ecBytes.length);
    for (int x = 0; x < expected.length; x++) {
      assertEquals(expected[x], ecBytes[x] & 0xFF);
    }
  }

  @Test
  public void testBugInBitVectorNumBytes() throws WriterException {
    // There was a bug in BitVector.sizeInBytes() that caused it to return a
    // smaller-by-one value (ex. 1465 instead of 1466) if the number of bits
    // in the vector is not 8-bit aligned.  In QRCodeEncoder::InitQRCode(),
    // BitVector::sizeInBytes() is used for finding the smallest QR Code
    // version that can fit the given data.  Hence there were corner cases
    // where we chose a wrong QR Code version that cannot fit the given
    // data.  Note that the issue did not occur with MODE_8BIT_BYTE, as the
    // bits in the bit vector are always 8-bit aligned.
    //
    // Before the bug was fixed, the following test didn't pass, because:
    //
    // - MODE_NUMERIC is chosen as all bytes in the data are '0'
    // - The 3518-byte numeric data needs 1466 bytes
    //   - 3518 / 3 * 10 + 7 = 11727 bits = 1465.875 bytes
    //   - 3 numeric bytes are encoded in 10 bits, hence the first
    //     3516 bytes are encoded in 3516 / 3 * 10 = 11720 bits.
    //   - 2 numeric bytes can be encoded in 7 bits, hence the last
    //     2 bytes are encoded in 7 bits.
    // - The version 27 QR Code with the EC level L has 1468 bytes for data.
    //   - 1828 - 360 = 1468
    // - In InitQRCode(), 3 bytes are reserved for a header.  Hence 1465 bytes
    //   (1468 -3) are left for data.
    // - Because of the bug in BitVector::sizeInBytes(), InitQRCode() determines
    //   the given data can fit in 1465 bytes, despite it needs 1466 bytes.
    // - Hence QRCodeEncoder.encode() failed and returned false.
    //   - To be precise, it needs 11727 + 4 (getMode info) + 14 (length info) =
    //     11745 bits = 1468.125 bytes are needed (i.e. cannot fit in 1468
    //     bytes).
    StringBuilder builder = new StringBuilder(3518);
    for (int x = 0; x < 3518; x++) {
      builder.append('0');
    }
    Encoder.encode(builder.toString(), ErrorCorrectionLevel.L);
  }

  @Test
  public void testMinimalEncoder1() throws Exception {
    verifyMinimalEncoding("A", "ALPHANUMERIC(A)", false);
  }

  @Test
  public void testMinimalEncoder2() throws Exception {
    verifyMinimalEncoding("AB", "ALPHANUMERIC(AB)", false);
  }

  @Test
  public void testMinimalEncoder3() throws Exception {
    verifyMinimalEncoding("ABC", "ALPHANUMERIC(ABC)", false);
  }

  @Test
  public void testMinimalEncoder4() throws Exception {
    verifyMinimalEncoding("ABCD", "ALPHANUMERIC(ABCD)", false);
  }

  @Test
  public void testMinimalEncoder5() throws Exception {
    verifyMinimalEncoding("ABCDE", "ALPHANUMERIC(ABCDE)", false);
  }

  @Test
  public void testMinimalEncoder6() throws Exception {
    verifyMinimalEncoding("ABCDEF", "ALPHANUMERIC(ABCDEF)", false);
  }

  @Test
  public void testMinimalEncoder7() throws Exception {
    verifyMinimalEncoding("ABCDEFG", "ALPHANUMERIC(ABCDEFG)", false);
  }

  @Test
  public void testMinimalEncoder8() throws Exception {
    verifyMinimalEncoding("1", "NUMERIC(1)", false);
  }

  @Test
  public void testMinimalEncoder9() throws Exception {
    verifyMinimalEncoding("12", "NUMERIC(12)", false);
  }

  @Test
  public void testMinimalEncoder10() throws Exception {
    verifyMinimalEncoding("123", "NUMERIC(123)", false);
  }

  @Test
  public void testMinimalEncoder11() throws Exception {
    verifyMinimalEncoding("1234", "NUMERIC(1234)", false);
  }

  @Test
  public void testMinimalEncoder12() throws Exception {
    verifyMinimalEncoding("12345", "NUMERIC(12345)", false);
  }

  @Test
  public void testMinimalEncoder13() throws Exception {
    verifyMinimalEncoding("123456", "NUMERIC(123456)", false);
  }

  @Test
  public void testMinimalEncoder14() throws Exception {
    verifyMinimalEncoding("123A", "ALPHANUMERIC(123A)", false);
  }

  @Test
  public void testMinimalEncoder15() throws Exception {
    verifyMinimalEncoding("A1", "ALPHANUMERIC(A1)", false);
  }

  @Test
  public void testMinimalEncoder16() throws Exception {
    verifyMinimalEncoding("A12", "ALPHANUMERIC(A12)", false);
  }

  @Test
  public void testMinimalEncoder17() throws Exception {
    verifyMinimalEncoding("A123", "ALPHANUMERIC(A123)", false);
  }

  @Test
  public void testMinimalEncoder18() throws Exception {
    verifyMinimalEncoding("A1234", "ALPHANUMERIC(A1234)", false);
  }

  @Test
  public void testMinimalEncoder19() throws Exception {
    verifyMinimalEncoding("A12345", "ALPHANUMERIC(A12345)", false);
  }

  @Test
  public void testMinimalEncoder20() throws Exception {
    verifyMinimalEncoding("A123456", "ALPHANUMERIC(A123456)", false);
  }

  @Test
  public void testMinimalEncoder21() throws Exception {
    verifyMinimalEncoding("A1234567", "ALPHANUMERIC(A1234567)", false);
  }

  @Test
  public void testMinimalEncoder22() throws Exception {
    verifyMinimalEncoding("A12345678", "BYTE(A),NUMERIC(12345678)", false);
  }

  @Test
  public void testMinimalEncoder23() throws Exception {
    verifyMinimalEncoding("A123456789", "BYTE(A),NUMERIC(123456789)", false);
  }

  @Test
  public void testMinimalEncoder24() throws Exception {
    verifyMinimalEncoding("A1234567890", "ALPHANUMERIC(A1),NUMERIC(234567890)", false);
  }

  @Test
  public void testMinimalEncoder25() throws Exception {
    verifyMinimalEncoding("AB1", "ALPHANUMERIC(AB1)", false);
  }

  @Test
  public void testMinimalEncoder26() throws Exception {
    verifyMinimalEncoding("AB12", "ALPHANUMERIC(AB12)", false);
  }

  @Test
  public void testMinimalEncoder27() throws Exception {
    verifyMinimalEncoding("AB123", "ALPHANUMERIC(AB123)", false);
  }

  @Test
  public void testMinimalEncoder28() throws Exception {
    verifyMinimalEncoding("AB1234", "ALPHANUMERIC(AB1234)", false);
  }

  @Test
  public void testMinimalEncoder29() throws Exception {
    verifyMinimalEncoding("ABC1", "ALPHANUMERIC(ABC1)", false);
  }

  @Test
  public void testMinimalEncoder30() throws Exception {
    verifyMinimalEncoding("ABC12", "ALPHANUMERIC(ABC12)", false);
  }

  @Test
  public void testMinimalEncoder31() throws Exception {
    verifyMinimalEncoding("ABC1234", "ALPHANUMERIC(ABC1234)", false);
  }

  @Test
  public void testMinimalEncoder32() throws Exception {
    verifyMinimalEncoding("http://foo.com", "BYTE(http://foo.com)", false);
  }

  @Test
  public void testMinimalEncoder33() throws Exception {
    verifyMinimalEncoding("HTTP://FOO.COM", "ALPHANUMERIC(HTTP://FOO.COM)", false);
  }

  @Test
  public void testMinimalEncoder34() throws Exception {
    verifyMinimalEncoding("1001114670010%01201220%107211220%140045003267781",
        "NUMERIC(1001114670010),ALPHANUMERIC(%01201220%107211220%),NUMERIC(140045003267781)", false);
  }

  @Test
  public void testMinimalEncoder35() throws Exception {
    verifyMinimalEncoding("\u0150", "ECI(ISO-8859-2),BYTE(.)", false);
  }

  @Test
  public void testMinimalEncoder36() throws Exception {
    verifyMinimalEncoding("\u015C", "ECI(ISO-8859-3),BYTE(.)", false);
  }

  @Test
  public void testMinimalEncoder37() throws Exception {
    verifyMinimalEncoding("\u0150\u015C", "ECI(UTF-8),BYTE(..)", false);
  }

  @Test
  public void testMinimalEncoder38() throws Exception {
    verifyMinimalEncoding("\u0150\u0150\u015C\u015C", "ECI(ISO-8859-2),BYTE(..),ECI(ISO-8859-3),BYTE(..)", false);
  }

  @Test
  public void testMinimalEncoder39() throws Exception {
    verifyMinimalEncoding("abcdef\u0150ghij", "ECI(ISO-8859-2),BYTE(abcdef.ghij)", false);
  }

  @Test
  public void testMinimalEncoder40() throws Exception {
    verifyMinimalEncoding("2938928329832983\u01502938928329832983\u015C2938928329832983",
        "NUMERIC(2938928329832983),ECI(ISO-8859-2),BYTE(.),NUMERIC(2938928329832983),ECI(ISO-8" +
        "859-3),BYTE(.),NUMERIC(2938928329832983)", false);
  }

  @Test
  public void testMinimalEncoder41() throws Exception {
    verifyMinimalEncoding("1001114670010%01201220%107211220%140045003267781", "FNC1_FIRST_POSITION(),NUMERIC(100111" +
        "4670010),ALPHANUMERIC(%01201220%107211220%),NUMERIC(140045003267781)",
        true);
  }

  @Test
  public void testMinimalEncoder42() throws Exception {
    // test halfwidth Katakana character (they are single byte encoded in Shift_JIS)
    verifyMinimalEncoding("Katakana:\uFF66\uFF66\uFF66\uFF66\uFF66\uFF66", "ECI(Shift_JIS),BYTE(Katakana:......)",
        false);
  }

  @Test
  public void testMinimalEncoder43() throws Exception {
    // The character \u30A2 encodes as double byte in Shift_JIS so KANJI is more compact in this case
    verifyMinimalEncoding("Katakana:\u30A2\u30A2\u30A2\u30A2\u30A2\u30A2", "BYTE(Katakana:),KANJI(......)", false);
  }

  @Test
  public void testMinimalEncoder44() throws Exception {
    // The character \u30A2 encodes as double byte in Shift_JIS but KANJI is not more compact in this case because
    // KANJI is only more compact when it encodes pairs of characters. In the case of mixed text it can however be
    // that Shift_JIS encoding is more compact as in this example
    verifyMinimalEncoding("Katakana:\u30A2a\u30A2a\u30A2a\u30A2a\u30A2a\u30A2",
        "ECI(Shift_JIS),BYTE(Katakana:.a.a.a.a.a.)", false);
  }

  static void verifyMinimalEncoding(String input, String expectedResult, boolean isGS1)
      throws Exception {
    int[] outVersion = new int[1];
    List<MinimalEncoder.ResultNode> result =
        MinimalEncoder.encode(input, null, isGS1, ErrorCorrectionLevel.L, outVersion);
    assertEquals(toString(result), expectedResult);
  }

  private static void verifyGS1EncodedData(QRCode qrCode) {
    String expected = "<<\n" +
        " mode: ALPHANUMERIC\n" +
        " ecLevel: H\n" +
        " version: 2\n" +
        " maskPattern: 2\n" +
        " matrix:\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 1 1 1 0 1 1 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 1 0 0 0 0 1 1 0 1 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 1 0 1 1 0 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 0 1 0 1 1 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 1 1 1 1 1 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 1 0 0 1 1 1 0 0 0 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 1 1 1 0 0 0 1 1 1 0 0 0 0 0 0 0 0\n" +
        " 0 0 1 1 1 0 1 0 1 1 1 1 0 1 1 0 1 1 1 1 0 0 1 1 1\n" +
        " 0 0 0 1 1 1 0 1 0 0 1 0 0 1 0 0 1 1 1 0 0 1 0 0 1\n" +
        " 1 0 1 1 0 0 1 0 1 1 0 0 0 0 1 0 1 1 1 0 0 1 0 0 1\n" +
        " 0 0 1 1 0 1 0 1 1 1 1 0 0 1 1 1 1 0 0 0 1 1 0 1 1\n" +
        " 0 0 1 0 0 0 1 0 0 0 1 1 0 1 0 0 0 1 0 1 1 1 0 1 0\n" +
        " 1 1 1 0 1 1 0 1 0 0 0 0 0 0 0 1 1 0 1 1 0 1 0 0 0\n" +
        " 1 0 1 0 1 0 1 1 0 1 0 1 0 1 1 0 0 0 0 0 1 1 0 0 1\n" +
        " 1 0 0 1 0 1 0 1 0 0 0 1 1 1 1 0 1 0 1 0 0 1 0 0 1\n" +
        " 1 0 1 0 0 1 1 1 0 1 1 0 0 1 0 0 1 1 1 1 1 1 0 0 0\n" +
        " 0 0 0 0 0 0 0 0 1 0 0 1 0 1 1 0 1 0 0 0 1 0 0 1 0\n" +
        " 1 1 1 1 1 1 1 0 0 0 0 1 0 0 1 1 1 0 1 0 1 0 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 1 1 1 1 0 1 1 0 0 0 1 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 0 0 1 1 1 1 1 1 1 1 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 1 0 0 0 0 0 0 0 0 1 0 1 0 0 0 0\n" +
        " 1 0 1 1 1 0 1 0 1 0 0 0 1 1 0 1 0 0 1 1 1 0 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 0 1 0 1 1 1 0 1 0 0 1 1 1 1 1\n" +
        " 1 1 1 1 1 1 1 0 0 1 1 0 0 1 1 0 1 0 0 0 0 1 0 1 1\n" +
        ">>\n";
    assertEquals(expected, QRCodeTestCase.toString(qrCode));
  }

  private static void verifyNotGS1EncodedData(QRCode qrCode) {
    String expected = "<<\n" +
        " mode: ALPHANUMERIC\n" +
        " ecLevel: H\n" +
        " version: 1\n" +
        " maskPattern: 0\n" +
        " matrix:\n" +
        " 1 1 1 1 1 1 1 0 1 1 1 1 0 0 1 1 1 1 1 1 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 1 1 0 0 1 0 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 0 1 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 1 1 0 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 1 0 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 0 0 0 0 1 0 0 0 0 0 1\n" +
        " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
        " 0 0 0 0 0 0 0 0 0 0 1 0 1 0 0 0 0 0 0 0 0\n" +
        " 0 0 1 0 1 1 1 0 1 1 0 0 1 1 0 0 0 1 0 0 1\n" +
        " 1 0 1 1 1 0 0 1 0 0 0 1 0 1 0 0 0 0 0 0 0\n" +
        " 0 0 1 1 0 0 1 0 1 0 0 0 1 0 1 0 1 0 1 1 0\n" +
        " 1 1 0 1 0 1 0 1 1 1 0 1 0 1 0 0 0 0 0 1 0\n" +
        " 0 0 1 1 0 1 1 1 1 0 0 0 1 0 1 0 1 1 1 1 0\n" +
        " 0 0 0 0 0 0 0 0 1 0 0 1 1 1 0 1 0 1 0 0 0\n" +
        " 1 1 1 1 1 1 1 0 0 0 1 0 1 0 1 1 0 0 0 0 1\n" +
        " 1 0 0 0 0 0 1 0 1 1 1 1 0 1 0 1 1 1 1 0 1\n" +
        " 1 0 1 1 1 0 1 0 1 0 1 1 0 1 0 1 0 0 0 0 1\n" +
        " 1 0 1 1 1 0 1 0 0 1 1 0 1 1 1 1 0 1 0 1 0\n" +
        " 1 0 1 1 1 0 1 0 1 0 0 0 1 0 1 0 1 1 1 0 1\n" +
        " 1 0 0 0 0 0 1 0 0 1 1 0 1 1 0 1 0 0 0 1 1\n" +
        " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 1 0 1 0 1\n" +
        ">>\n";
    assertEquals(expected, QRCodeTestCase.toString(qrCode));
  }

  private static String shiftJISString(byte[] bytes) {
    return new String(bytes, Encoder.SHIFT_JIS_CHARSET);
  }

  private static String toString(List<MinimalEncoder.ResultNode> list) { // Mike-CHANGED parameters
    StringBuilder result = new StringBuilder();
    MinimalEncoder.ResultNode previous = null;
    for (MinimalEncoder.ResultNode current : list) {
      if (previous != null) {
        result.append(",");
      }
      appendTo(current, result);
      previous = current;
    }
    return result.toString();
  }
  private static void appendTo(MinimalEncoder.ResultNode node, StringBuilder result) { // Mike-REWORKED: inlined makePrintable, eliminated extra StringBuilder
    Mode mode = node.mode;
    result.append(mode.name()).append('(');
    if (mode == Mode.ECI) {
      result.append(node.encoder.charset().displayName());
    } else {
      String stringToEncode = node.stringToEncode;
      for (int i = node.fromPosition; i < node.fromPosition + node.characterLength; i++) {
        result.append(
            stringToEncode.charAt(i) < 32 || stringToEncode.charAt(i) > 126 ? '.' : stringToEncode.charAt(i));
      }
    }
    result.append(')');
  }

}
