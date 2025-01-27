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
 * <p>See ISO 18004:2006, 6.5.1. This enum encapsulates the four error correction levels
 * defined by the QR code standard.</p>
 *
 * @author Sean Owen
 */
public enum ErrorCorrectionLevel {

  // Mike-REORDERED so ordinal() = bits
  /** M = ~15% correction */
  M,
  /** L = ~7% correction */
  L,
  /** H = ~30% correction */
  H,
  /** Q = ~25% correction */
  Q,

  // Mike-REMOVED FOR_BITS, bits, ErrorCorrectionLevel(int), getBits, forBits


}
