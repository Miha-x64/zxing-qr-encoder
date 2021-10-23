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

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class QRCode {

  public static final int NUM_MASK_PATTERNS = 8;

  // Mike-CHANGED: everything is public final
  public final Mode mode;
  public final ErrorCorrectionLevel ecLevel;
  public final Version version;
  public final int maskPattern;
  public final ByteMatrix matrix;

  // Mike-CHANGED no-arg constructor to all-args
  public QRCode(Mode mode, ErrorCorrectionLevel ecLevel, Version version, int maskPattern, ByteMatrix matrix) {
    if (mode == null || ecLevel == null || version == null || !isValidMaskPattern(maskPattern) || matrix == null)
      throw new IllegalArgumentException();
    this.mode = mode;
    this.ecLevel = ecLevel;
    this.version = version;
    this.maskPattern = maskPattern;
    this.matrix = matrix;
  }

  // Mike-REMOVED getters

  @Override
  public String toString() { // Mike-CHANGED SB to concat
    return "<<\n" +
        " mode: " + mode +
        "\n ecLevel: " + ecLevel +
        "\n version: " + version +
        "\n maskPattern: " + maskPattern +
        "\n matrix:\n" + matrix +
        ">>\n";
  }

  // Mike-REMOVED setters

  // Check if "mask_pattern" is valid.
  public static boolean isValidMaskPattern(int maskPattern) {
    return maskPattern >= 0 && maskPattern < NUM_MASK_PATTERNS;
  }

}
