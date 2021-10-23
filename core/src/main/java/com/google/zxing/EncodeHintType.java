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

package com.google.zxing;

/**
 * These are a set of hints that you may pass to Writers to specify their behavior.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public enum EncodeHintType {

  /**
   * Specifies what degree of error correction to use, for example in QR Codes.
   * Type depends on the encoder. For example for QR codes it's type
   * {@link com.google.zxing.qrcode.decoder.ErrorCorrectionLevel ErrorCorrectionLevel}.
   * For Aztec it is of type {@link Integer}, representing the minimal percentage of error correction words.
   * For PDF417 it is of type {@link Integer}, valid values being 0 to 8.
   * In all cases, it can also be a {@link String} representation of the desired value as well.
   * Note: an Aztec symbol should have a minimum of 25% EC words.
   */
  ERROR_CORRECTION,

  /**
   * Specifies what character encoding to use where applicable (type {@link String})
   */
  CHARACTER_SET,

  // Mike-REMOVED DATA_MATRIX_SHAPE, DATA_MATRIX_SHAPE, MAX_SIZE

  /**
   * Specifies margin, in pixels, to use when generating the barcode. The meaning can vary
   * by format; for example it controls margin before and after the barcode horizontally for
   * most 1D formats. (Type {@link Integer}, or {@link String} representation of the integer value).
   */
  MARGIN,

  // Mike-REMOVED PDF417_* and AZTEC_LAYERS

   /**
    * Specifies the exact version of QR code to be encoded.
    * (Type {@link Integer}, or {@link String} representation of the integer value).
    */
   QR_VERSION,

  /**
   * Specifies the QR code mask pattern to be used. Allowed values are
   * 0..QRCode.NUM_MASK_PATTERNS-1. By default the code will automatically select
   * the optimal mask pattern.
   * * (Type {@link Integer}, or {@link String} representation of the integer value).
   */
  QR_MASK_PATTERN,


  /**
   * Specifies whether to use compact mode for QR code (type {@link Boolean}, or "true" or "false"
   * Please note that when compaction is performed, the most compact character encoding is chosen
   * for characters in the input that are not in the ISO-8859-1 character set. Based on experience,
   * some scanners do not support encodings like cp-1256 (Arabic). In such cases the encoding can
   * be forced to UTF-8 by means of the {@link #CHARACTER_SET} encoding hint.
   */
  QR_COMPACT,

  /**
   * Specifies whether the data should be encoded to the GS1 standard (type {@link Boolean}, or "true" or "false"
   * {@link String } value).
   */
  GS1_FORMAT,

  // Mike-REMOVED FORCE_CODE_SET

}
