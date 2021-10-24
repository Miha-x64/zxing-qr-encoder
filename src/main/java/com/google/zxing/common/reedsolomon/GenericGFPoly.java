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

package com.google.zxing.common.reedsolomon;

/**
 * <p>Represents a polynomial whose coefficients are elements of a GF.
 * Instances of this class are immutable.</p>
 *
 * <p>Much credit is due to William Rucklidge since portions of this code are an indirect
 * port of his C++ Reed-Solomon implementation.</p>
 *
 * @author Sean Owen
 */
final class GenericGFPoly {

  private final GenericGF field;
  final int[] coefficients; // Mike-CHANGED: unprivated

  /**
   * @param field the {@link GenericGF} instance representing the field to use
   * to perform computations
   * @param coefficients coefficients as ints representing elements of GF(size), arranged
   * from most significant (highest-power term) coefficient to least significant
   * @throws IllegalArgumentException if argument is null or empty,
   * or if leading coefficient is 0 and this is not a
   * constant polynomial (that is, it is not the monomial "0")
   */
  GenericGFPoly(GenericGF field, int[] coefficients) {
    if (coefficients.length == 0) {
      throw new IllegalArgumentException();
    }
    this.field = field;
    int coefficientsLength = coefficients.length;
    if (coefficientsLength > 1 && coefficients[0] == 0) {
      // Leading term must be non-zero for anything except the constant polynomial "0"
      int firstNonZero = 1;
      while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
        firstNonZero++;
      }
      if (firstNonZero == coefficientsLength) {
        this.coefficients = new int[]{0};
      } else {
        this.coefficients = new int[coefficientsLength - firstNonZero];
        System.arraycopy(coefficients,
            firstNonZero,
            this.coefficients,
            0,
            this.coefficients.length);
      }
    } else {
      this.coefficients = coefficients;
    }
  }

  // Mike-REMOVED getCoefficients

  /**
   * @return degree of this polynomial
   */
  int getDegree() {
    return coefficients.length - 1;
  }

  /**
   * @return true iff this polynomial is the monomial "0"
   */
  boolean isZero() {
    return coefficients[0] == 0;
  }

  /**
   * @return coefficient of x^degree term in this polynomial
   */
  int getCoefficient(int degree) {
    return coefficients[coefficients.length - 1 - degree];
  }

  // Mike-REMOVED evaluateAt

  GenericGFPoly addOrSubtract(GenericGFPoly other) {
    if (!field.equals(other.field)) {
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
    }
    if (isZero()) {
      return other;
    }
    if (other.isZero()) {
      return this;
    }

    int[] smallerCoefficients = this.coefficients;
    int[] largerCoefficients = other.coefficients;
    if (smallerCoefficients.length > largerCoefficients.length) {
      int[] temp = smallerCoefficients;
      smallerCoefficients = largerCoefficients;
      largerCoefficients = temp;
    }
    int[] sumDiff = new int[largerCoefficients.length];
    int lengthDiff = largerCoefficients.length - smallerCoefficients.length;
    // Copy high-order terms only found in higher-degree polynomial's coefficients
    System.arraycopy(largerCoefficients, 0, sumDiff, 0, lengthDiff);

    for (int i = lengthDiff; i < largerCoefficients.length; i++) {
      sumDiff[i] = smallerCoefficients[i - lengthDiff] ^ largerCoefficients[i]; // Mike-CHANGED: inlined addOrSubtract
    }

    return new GenericGFPoly(field, sumDiff);
  }

  GenericGFPoly multiply(GenericGFPoly other) {
    if (!field.equals(other.field)) {
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
    }
    if (isZero() || other.isZero()) {
      return field.zero;
    }
    int[] aCoefficients = this.coefficients;
    int aLength = aCoefficients.length;
    int[] bCoefficients = other.coefficients;
    int bLength = bCoefficients.length;
    int[] product = new int[aLength + bLength - 1];
    for (int i = 0; i < aLength; i++) {
      int aCoeff = aCoefficients[i];
      for (int j = 0; j < bLength; j++) {
        product[i + j] = product[i + j] ^ field.multiply(aCoeff, bCoefficients[j]); // Mike-CHANGED: inlined addOrSubtract
      }
    }
    return new GenericGFPoly(field, product);
  }

  // Mike-REMOVED multiply

  GenericGFPoly multiplyByMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return field.zero;
    }
    int size = coefficients.length;
    int[] product = new int[size + degree];
    for (int i = 0; i < size; i++) {
      product[i] = field.multiply(coefficients[i], coefficient);
    }
    return new GenericGFPoly(field, product);
  }

  // Mike-CHANGED: removed quotient calculation, returning only remainder
  GenericGFPoly remainder(GenericGFPoly other) {
    if (!field.equals(other.field)) {
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
    }
    if (other.isZero()) {
      throw new IllegalArgumentException("Divide by 0");
    }

    GenericGFPoly remainder = this;

    int denominatorLeadingTerm = other.getCoefficient(other.getDegree());
    int inverseDenominatorLeadingTerm = field.inverse(denominatorLeadingTerm);

    while (remainder.getDegree() >= other.getDegree() && !remainder.isZero()) {
      int degreeDifference = remainder.getDegree() - other.getDegree();
      int scale = field.multiply(remainder.getCoefficient(remainder.getDegree()), inverseDenominatorLeadingTerm);
      GenericGFPoly term = other.multiplyByMonomial(degreeDifference, scale);
      remainder = remainder.addOrSubtract(term);
    }

    return remainder;
  }

  // Mike-REMOVED toString

}
