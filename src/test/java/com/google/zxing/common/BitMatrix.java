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

package com.google.zxing.common;

import java.util.Arrays;

import static com.google.zxing.common.BitArrayUtils.clear;
import static com.google.zxing.common.BitArrayUtils.setBulk;

/**
 * <p>Represents a 2D matrix of bits. In function arguments below, and throughout the common
 * module, x is the column position, and y is the row position. The ordering is always x, y.
 * The origin is at the top-left.</p>
 *
 * <p>Internally the bits are represented in a 1-D array of 32-bit ints. However, each row begins
 * with a new int. This is done intentionally so that we can copy out a row into a BitArray very
 * efficiently.</p>
 *
 * <p>The ordering of bits is row-major. Within each int, the least significant bits are used first,
 * meaning they represent lower x values. This is compatible with BitArray's implementation.</p>
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class BitMatrix implements Cloneable { // Mike-MOVED to test/

  private int width;
  private int height;
  private int rowSize;
  private int[] bits;

  /**
   * Creates an empty square {@code BitMatrix}.
   *
   * @param dimension height and width
   */
  public BitMatrix(int dimension) {
    this(dimension, dimension);
  }

  /**
   * Creates an empty {@code BitMatrix}.
   *
   * @param width bit matrix width
   * @param height bit matrix height
   */
  public BitMatrix(int width, int height) {
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Both dimensions must be greater than 0");
    }
    this.width = width;
    this.height = height;
    this.rowSize = (width + 31) / 32;
    bits = new int[rowSize * height];
  }

  private BitMatrix(int width, int height, int rowSize, int[] bits) {
    this.width = width;
    this.height = height;
    this.rowSize = rowSize;
    this.bits = bits;
  }

  // Mike-REMOVED parse

  /**
   * <p>Gets the requested bit, where true means black.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   * @return value of given bit in matrix
   */
  public boolean get(int x, int y) {
    int offset = y * rowSize + (x / 32);
    return ((bits[offset] >>> (x & 0x1f)) & 1) != 0;
  }

  /**
   * <p>Sets the given bit to true.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void set(int x, int y) {
    int offset = y * rowSize + (x / 32);
    bits[offset] |= 1 << (x & 0x1f);
  }

  public void unset(int x, int y) {
    int offset = y * rowSize + (x / 32);
    bits[offset] &= ~(1 << (x & 0x1f));
  }

  /**
   * <p>Flips the given bit.</p>
   *
   * @param x The horizontal component (i.e. which column)
   * @param y The vertical component (i.e. which row)
   */
  public void flip(int x, int y) {
    int offset = y * rowSize + (x / 32);
    bits[offset] ^= 1 << (x & 0x1f);
  }

  // Mike-REMOVED flip

  /**
   * Exclusive-or (XOR): Flip the bit in this {@code BitMatrix} if the corresponding
   * mask bit is set.
   *
   * @param mask XOR mask
   */
  public void xor(BitMatrix mask) {
    if (width != mask.width || height != mask.height || rowSize != mask.rowSize) {
      throw new IllegalArgumentException("input matrix dimensions do not match");
    }
    BitArray rowArray = new BitArray(width);
    for (int y = 0; y < height; y++) {
      int offset = y * rowSize;
      int[] row = mask.getRow(y, rowArray).getBitArray();
      for (int x = 0; x < rowSize; x++) {
        bits[offset + x] ^= row[x];
      }
    }
  }

  // Mike-REMOVED clear

  /**
   * <p>Sets a square region of the bit matrix to true.</p>
   *
   * @param left The horizontal position to begin at (inclusive)
   * @param top The vertical position to begin at (inclusive)
   * @param width The width of the region
   * @param height The height of the region
   */
  public void setRegion(int left, int top, int width, int height) {
    if (top < 0 || left < 0) {
      throw new IllegalArgumentException("Left and top must be nonnegative");
    }
    if (height < 1 || width < 1) {
      throw new IllegalArgumentException("Height and width must be at least 1");
    }
    int right = left + width;
    int bottom = top + height;
    if (bottom > this.height || right > this.width) {
      throw new IllegalArgumentException("The region must fit inside the matrix");
    }
    for (int y = top; y < bottom; y++) {
      int offset = y * rowSize;
      for (int x = left; x < right; x++) {
        bits[offset + (x / 32)] |= 1 << (x & 0x1f);
      }
    }
  }

  /**
   * A fast method to retrieve one row of data from the matrix as a BitArray.
   *
   * @param y The row to retrieve
   * @param row An optional caller-allocated BitArray, will be allocated if null or too small
   * @return The resulting BitArray - this reference should always be used even when passing
   *         your own row
   */
  public BitArray getRow(int y, BitArray row) {
    if (row == null || row.getSize() < width) {
      row = new BitArray(width);
    } else {
      clear(row);
    }
    int offset = y * rowSize;
    for (int x = 0; x < rowSize; x++) {
      setBulk(row, x * 32, bits[offset + x]);
    }
    return row;
  }

  // Mike-REMOVED setRow, rotate180

  /**
   * Modifies this {@code BitMatrix} to represent the same but rotated 90 degrees counterclockwise
   */
  public void rotate90() {
    int newWidth = height;
    int newHeight = width;
    int newRowSize = (newWidth + 31) / 32;
    int[] newBits = new int[newRowSize * newHeight];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int offset = y * rowSize + (x / 32);
        if (((bits[offset] >>> (x & 0x1f)) & 1) != 0) {
          int newOffset = (newHeight - 1 - x) * newRowSize + (y / 32);
          newBits[newOffset] |= 1 << (y & 0x1f);
        }
      }
    }
    width = newWidth;
    height = newHeight;
    rowSize = newRowSize;
    bits = newBits;
  }

  /**
   * This is useful in detecting the enclosing rectangle of a 'pure' barcode.
   *
   * @return {@code left,top,width,height} enclosing rectangle of all 1 bits, or null if it is all white
   */
  public int[] getEnclosingRectangle() {
    int left = width;
    int top = height;
    int right = -1;
    int bottom = -1;

    for (int y = 0; y < height; y++) {
      for (int x32 = 0; x32 < rowSize; x32++) {
        int theBits = bits[y * rowSize + x32];
        if (theBits != 0) {
          if (y < top) {
            top = y;
          }
          if (y > bottom) {
            bottom = y;
          }
          if (x32 * 32 < left) {
            int bit = 0;
            while ((theBits << (31 - bit)) == 0) {
              bit++;
            }
            if ((x32 * 32 + bit) < left) {
              left = x32 * 32 + bit;
            }
          }
          if (x32 * 32 + 31 > right) {
            int bit = 31;
            while ((theBits >>> bit) == 0) {
              bit--;
            }
            if ((x32 * 32 + bit) > right) {
              right = x32 * 32 + bit;
            }
          }
        }
      }
    }

    if (right < left || bottom < top) {
      return null;
    }

    return new int[] {left, top, right - left + 1, bottom - top + 1};
  }

  /**
   * This is useful in detecting a corner of a 'pure' barcode.
   *
   * @return {@code x,y} coordinate of top-left-most 1 bit, or null if it is all white
   */
  public int[] getTopLeftOnBit() {
    int bitsOffset = 0;
    while (bitsOffset < bits.length && bits[bitsOffset] == 0) {
      bitsOffset++;
    }
    if (bitsOffset == bits.length) {
      return null;
    }
    int y = bitsOffset / rowSize;
    int x = (bitsOffset % rowSize) * 32;

    int theBits = bits[bitsOffset];
    int bit = 0;
    while ((theBits << (31 - bit)) == 0) {
      bit++;
    }
    x += bit;
    return new int[] {x, y};
  }

  public int[] getBottomRightOnBit() {
    int bitsOffset = bits.length - 1;
    while (bitsOffset >= 0 && bits[bitsOffset] == 0) {
      bitsOffset--;
    }
    if (bitsOffset < 0) {
      return null;
    }

    int y = bitsOffset / rowSize;
    int x = (bitsOffset % rowSize) * 32;

    int theBits = bits[bitsOffset];
    int bit = 31;
    while ((theBits >>> bit) == 0) {
      bit--;
    }
    x += bit;

    return new int[] {x, y};
  }

  /**
   * @return The width of the matrix
   */
  public int getWidth() {
    return width;
  }

  /**
   * @return The height of the matrix
   */
  public int getHeight() {
    return height;
  }

  // Mike-REMOVED getRowSize

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BitMatrix)) {
      return false;
    }
    BitMatrix other = (BitMatrix) o;
    return width == other.width && height == other.height && rowSize == other.rowSize &&
    Arrays.equals(bits, other.bits);
  }

  @Override
  public int hashCode() {
    int hash = width;
    hash = 31 * hash + width;
    hash = 31 * hash + height;
    hash = 31 * hash + rowSize;
    hash = 31 * hash + Arrays.hashCode(bits);
    return hash;
  }

  // Mike-REMOVED toString

  @Override
  public BitMatrix clone() {
    return new BitMatrix(width, height, rowSize, bits.clone());
  }

}
