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

/**
 * @author Satoru Takabayashi
 * @author Daniel Switkin
 * @author Sean Owen
 */
final class MaskUtil {

  // Mike-REMOVED constants, now they are inlined

  private MaskUtil() {
    // do nothing
  }

  // Mike-REMOVED applyMaskPenaltyRule1

  /**
   * Apply mask penalty rule 2 and return the penalty. Find 2x2 blocks with the same color and give
   * penalty to them. This is actually equivalent to the spec's rule, which is to find MxN blocks and give a
   * penalty proportional to (M-1)x(N-1), because this is the number of 2x2 blocks inside such a block.
   */
  static int applyMaskPenaltyRule2(ByteMatrix matrix) {
    int penalty = 0;
    int width = matrix.width; // Mike-CHANGED: direct w/h field access; matrix.get() accessor
    int height = matrix.height;
    for (int y = 0; y < height - 1; y++) {
      for (int x = 0; x < width - 1; x++) {
        int value = matrix.get(x, y);
        if (value == matrix.get(x + 1, y) && value == matrix.get(x, y + 1) && value == matrix.get(x + 1, y + 1)) {
          penalty++;
        }
      }
    }
    return 3 * penalty; // Mike-CHANGED inlined constant
  }

  /**
   * Apply mask penalty rule 3 and return the penalty. Find consecutive runs of 1:1:3:1:1:4
   * starting with black, or 4:1:1:3:1:1 starting with white, and give penalty to them.  If we
   * find patterns like 000010111010000, we give penalty once.
   */
  static int applyMaskPenaltyRule3(ByteMatrix matrix) {
    int numPenalties = 0;
    int width = matrix.width; // Mike-CHANGED: direct w/h field access; matrix.get() accessor
    int height = matrix.height;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (x + 6 < width &&
            matrix.get(x, y) == 1 &&
            matrix.get(x + 1, y) == 0 &&
            matrix.get(x + 2, y) == 1 &&
            matrix.get(x + 3, y) == 1 &&
            matrix.get(x + 4, y) == 1 &&
            matrix.get(x + 5, y) == 0 &&
            matrix.get(x + 6, y) == 1 &&
            (isWhiteHorizontal(matrix, y, x - 4, x) || isWhiteHorizontal(matrix, y, x + 7, x + 11))) {
          numPenalties++;
        }
        if (y + 6 < height &&
            matrix.get(x, y) == 1 &&
            matrix.get(x, y + 1) == 0 &&
            matrix.get(x, y + 2) == 1 &&
            matrix.get(x, y + 3) == 1 &&
            matrix.get(x, y + 4) == 1 &&
            matrix.get(x, y + 5) == 0 &&
            matrix.get(x, y + 6) == 1 &&
            (isWhiteVertical(matrix, x, y - 4, y) || isWhiteVertical(matrix, x, y + 7, y + 11))) {
          numPenalties++;
        }
      }
    }
    return numPenalties * 40; // Mike-CHANGED inlined constant
  }

  // Mike-CHANGED: accepting full matrix and using accessors
  private static boolean isWhiteHorizontal(ByteMatrix matrix, int y, int from, int to) {
    if (from < 0 || matrix.width < to) {
      return false;
    }
    for (int x = from; x < to; x++) {
      if (matrix.get(x, y) == 1) {
        return false;
      }
    }
    return true;
  }

  // Mike-CHANGED: using matrix accessors
  private static boolean isWhiteVertical(ByteMatrix matrix, int x, int from, int to) {
    if (from < 0 || matrix.height < to) {
      return false;
    }
    for (int y = from; y < to; y++) {
      if (matrix.get(x, y) == 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * Apply mask penalty rule 4 and return the penalty. Calculate the ratio of dark cells and give
   * penalty if the ratio is far from 50%. It gives 10 penalty for 5% distance.
   */
  static int applyMaskPenaltyRule4(ByteMatrix matrix) {
    int numDarkCells = 0;
    int width = matrix.width;// Mike-CHANGED: direct w/h access and use of get() accessor
    int height = matrix.height;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (matrix.get(x, y) == 1) {
          numDarkCells++;
        }
      }
    }
    int numTotalCells = matrix.height * matrix.width;
    int fivePercentVariances = Math.abs(numDarkCells * 2 - numTotalCells) * 10 / numTotalCells;
    return fivePercentVariances * 10; // Mike-CHANGED inlined constant
  }

  /**
   * Return the mask bit for "getMaskPattern" at "x" and "y". See 8.8 of JISX0510:2004 for mask
   * pattern conditions.
   */
  static boolean getDataMaskBit(int maskPattern, int x, int y) {
    int intermediate;
    int temp;
    switch (maskPattern) {
      case 0:
        intermediate = (y + x) & 0x1;
        break;
      case 1:
        intermediate = y & 0x1;
        break;
      case 2:
        intermediate = x % 3;
        break;
      case 3:
        intermediate = (y + x) % 3;
        break;
      case 4:
        intermediate = ((y / 2) + (x / 3)) & 0x1;
        break;
      case 5:
        temp = y * x;
        intermediate = (temp & 0x1) + (temp % 3);
        break;
      case 6:
        temp = y * x;
        intermediate = ((temp & 0x1) + (temp % 3)) & 0x1;
        break;
      case 7:
        temp = y * x;
        intermediate = ((temp % 3) + ((y + x) & 0x1)) & 0x1;
        break;
      default:
        throw new IllegalArgumentException("Invalid mask pattern: " + maskPattern);
    }
    return intermediate == 0;
  }

  /**
   * Helper function for applyMaskPenaltyRule1. We need this for doing this calculation in both
   * vertical and horizontal orders respectively.
   */
  static int applyMaskPenaltyRule1Internal(ByteMatrix matrix, boolean isHorizontal) {
    int penalty = 0; // Mike-CHANGED: direct w/h access and use of get() accessor
    int iLimit = isHorizontal ? matrix.height : matrix.width;
    int jLimit = isHorizontal ? matrix.width : matrix.height;
    for (int i = 0; i < iLimit; i++) {
      int numSameBitCells = 0;
      int prevBit = -1;
      for (int j = 0; j < jLimit; j++) {
        int bit = isHorizontal ? matrix.get(j, i) : matrix.get(i, j);
        if (bit == prevBit) {
          numSameBitCells++;
        } else {
          if (numSameBitCells >= 5) {
            penalty += 3 + (numSameBitCells - 5); // Mike-CHANGED inlined constant
          }
          numSameBitCells = 1;  // Include the cell itself.
          prevBit = bit;
        }
      }
      if (numSameBitCells >= 5) {
        penalty += 3 + (numSameBitCells - 5); // Mike-CHANGED inlined constant
      }
    }
    return penalty;
  }

}
