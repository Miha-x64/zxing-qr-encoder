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

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static com.google.zxing.common.BitArrayUtils.getNextSet;
import static com.google.zxing.common.BitArrayUtils.isRange;
import static com.google.zxing.common.BitArrayUtils.set;
import static com.google.zxing.common.BitArrayUtils.setBulk;
import static com.google.zxing.common.BitArrayUtils.setRange;

/**
 * @author Sean Owen
 */
public final class BitArrayTestCase extends Assert {

  @Test
  public void testGetSet() {
    BitArray array = new BitArray(33);
    for (int i = 0; i < 33; i++) {
      assertFalse(array.get(i));
      set(array, i);
      assertTrue(array.get(i));
    }
  }

  @Test
  public void testGetNextSet1() {
    BitArray array = new BitArray(32);
    for (int i = 0; i < array.getSize(); i++) {
      assertEquals(String.valueOf(i), 32, getNextSet(array, i));
    }
    array = new BitArray(33);
    for (int i = 0; i < array.getSize(); i++) {
      assertEquals(String.valueOf(i), 33, getNextSet(array, i));
    }
  }
  
  @Test
  public void testGetNextSet2() {
    BitArray array = new BitArray(33);
    set(array, 31);
    for (int i = 0; i < array.getSize(); i++) {
      assertEquals(String.valueOf(i), i <= 31 ? 31 : 33, getNextSet(array, i));
    }
    array = new BitArray(33);
    set(array, 32);
    for (int i = 0; i < array.getSize(); i++) {
      assertEquals(String.valueOf(i), 32, getNextSet(array, i));
    }
  }
  
  @Test
  public void testGetNextSet3() {
    BitArray array = new BitArray(63);
    set(array, 31);
    set(array, 32);
    for (int i = 0; i < array.getSize(); i++) {
      int expected;
      if (i <= 31) {
        expected = 31;
      } else if (i == 32) {
        expected = 32;
      } else {
        expected = 63;
      }
      assertEquals(String.valueOf(i), expected, getNextSet(array, i));
    }
  }

  @Test
  public void testGetNextSet4() {
    BitArray array = new BitArray(63);
    set(array, 33);
    set(array, 40);
    for (int i = 0; i < array.getSize(); i++) {
      int expected;
      if (i <= 33) {
        expected = 33;
      } else if (i <= 40) {
        expected = 40;
      } else {
        expected = 63;
      }
      assertEquals(String.valueOf(i), expected, getNextSet(array, i));
    }
  }
  
  @Test
  public void testGetNextSet5() {
    Random r = new Random(0xDEADBEEF);
    for (int i = 0; i < 10; i++) {
      BitArray array = new BitArray(1 + r.nextInt(100));
      int numSet = r.nextInt(20);
      for (int j = 0; j < numSet; j++) {
        set(array, r.nextInt(array.getSize()));
      }
      int numQueries = r.nextInt(20);
      for (int j = 0; j < numQueries; j++) {
        int query = r.nextInt(array.getSize());
        int expected = query;
        while (expected < array.getSize() && !array.get(expected)) {
          expected++;
        }
        int actual = getNextSet(array, query);
        assertEquals(expected, actual);
      }
    }
  }


  @Test
  public void testSetBulk() {
    BitArray array = new BitArray(64);
    setBulk(array, 32, 0xFFFF0000);
    for (int i = 0; i < 48; i++) {
      assertFalse(array.get(i));
    }
    for (int i = 48; i < 64; i++) {
      assertTrue(array.get(i));
    }
  }

  @Test
  public void testSetRange() {
    BitArray array = new BitArray(64);
    setRange(array, 28, 36);
    assertFalse(array.get(27));
    for (int i = 28; i < 36; i++) {
      assertTrue(array.get(i));
    }
    assertFalse(array.get(36));
  }

  // Mike-REMOVED testClear, testFlip

  @Test
  public void testGetArray() {
    BitArray array = new BitArray(64);
    set(array, 0);
    set(array, 63);
    int[] ints = array.getBitArray();
    assertEquals(1, ints[0]);
    assertEquals(Integer.MIN_VALUE, ints[1]);
  }

  @Test
  public void testIsRange() {
    BitArray array = new BitArray(64);
    assertTrue(isRange(array, 0, 64, false));
    assertFalse(isRange(array, 0, 64, true));
    set(array, 32);
    assertTrue(isRange(array, 32, 33, true));
    set(array, 31);
    assertTrue(isRange(array, 31, 33, true));
    set(array, 34);
    assertFalse(isRange(array, 31, 35, true));
    for (int i = 0; i < 31; i++) {
      set(array, i);
    }
    assertTrue(isRange(array, 0, 33, true));
    for (int i = 33; i < 64; i++) {
      set(array, i);
    }
    assertTrue(isRange(array, 0, 64, true));
    assertFalse(isRange(array, 0, 64, false));
  }

  // Mike-REMOVED reverseAlgorithmTest, testClone, testEquals, reverseOriginal, bitSet, arraysAreEqual

}
