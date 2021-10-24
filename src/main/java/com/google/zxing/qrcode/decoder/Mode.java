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
 * <p>See ISO 18004:2006, 6.4.1, Tables 2 and 3. This enum encapsulates the various modes in which
 * data can be encoded to bits in the QR code standard.</p>
 *
 * @author Sean Owen
 */
public enum Mode {

  // Mike-REMOVED TERMINATOR, STRUCTURED_APPEND, FNC1_SECOND_POSITION, HANZI
  // Mike-REORDERED for MinimalEncoder
  KANJI(8, 10, 12, 0x08),
  ALPHANUMERIC(9, 11, 13, 0x02),
  NUMERIC(10, 12, 14, 0x01),
  BYTE(8, 16, 16, 0x04),
  ECI(0, 0, 0, 0x07), // character counts don't apply
  FNC1_FIRST_POSITION(0, 0, 0, 0x05);

  // Mike-CHANGED to hold a single int
  private final int data;

  Mode(int small, int medium, int large, int bits) {
    this.data = (small << 24) | (medium << 16) | (large << 8) | bits;
  }

  // Mike-REMOVED forBits

  /**
   * @param version version in question
   * @return number of bits used, in this QR Code symbol {@link Version}, to encode the
   *         count of characters that will follow encoded in this Mode
   */
  public int getCharacterCountBits(int version) {
    if (version <= 9) {
      return data >>> 24;
    } else if (version <= 26) {
      return (data >>> 16) & 0xFF;
    } else {
      return (data >>> 8) & 0xFF;
    }
  }

  public int getBits() {
    return data & 0xFF;
  }
  // END Mike-CHANGED

}
