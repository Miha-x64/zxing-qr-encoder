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

import java.util.Arrays;

/**
 * JAVAPORT: The original code was a 2D array of ints, but since it only ever gets assigned
 * -1, 0, and 1, I'm going to use less memory and go with bytes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ByteMatrix {

  // Mike: added widthInts, flattened the array, made w/h public
  private final int widthInts;
  private final int[] matrix;
  public final int width;
  public final int height;

  public ByteMatrix(int width, int height) {
    this.widthInts = width / 16 + ((width % 16) > 0 ? 1 : 0);
    this.matrix = new int[height * widthInts];
    this.width = width;
    this.height = height;
  }

  // Mike-REMOVED: getWidth, getHeight

  public byte get(int x, int y) {
    // Mike-CHANGED: unpacking from array
    int value = ((matrix[widthInts*y + (x>>4)] >>> ((x & 15) << 1)) & 3);
    return (byte) ((value ^ 2) - 2); // sign extend
  }

  // Mike-REMOVED set(int, int, byte)

  public void set(int x, int y, int value) {
    // Mike-CHANGED: packing to array
    int shift = (x & 15) << 1;
    int index = widthInts * y + (x >> 4);
    matrix[index] = (matrix[index] & ~(3 << shift)) | ((value & 3) << shift);
  }

  public void set(int x, int y, boolean value) {
    set(x, y, value ? 1 : 0); // Mike-CHANGED: delegated to overload
  }

  public void clear(byte value) {
    int val = (value & 3); // Mike-CHANGED: filling single array
    val = val << 2 | val;
    val = val << 4 | val;
    val = val << 8 | val;
    val = val << 16 | val;
    Arrays.fill(matrix, val);
  }

  // Mike-REMOVED toString

}
