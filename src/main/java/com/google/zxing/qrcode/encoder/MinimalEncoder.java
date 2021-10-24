/*
 * Copyright 2021 ZXing authors
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

import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Encoder that encodes minimally
 *
 * Algorithm:
 *
 * The eleventh commandment was "Thou Shalt Compute" or "Thou Shalt Not Compute" - I forget which (Alan Perilis).
 *
 * This implementation computes. As an alternative, the QR-Code specification suggests heuristics like this one:
 *
 * If initial input data is in the exclusive subset of the Alphanumeric character set AND if there are less than
 * [6,7,8] characters followed by data from the remainder of the 8-bit byte character set, THEN select the 8-
 * bit byte mode ELSE select Alphanumeric mode;
 *
 * This is probably right for 99.99% of cases but there is at least this one counter example: The string "AAAAAAa"
 * encodes 2 bits smaller as ALPHANUMERIC(AAAAAA), BYTE(a) than by encoding it as BYTE(AAAAAAa).
 * Perhaps that is the only counter example but without having proof, it remains unclear.
 *
 * ECI switching:
 *
 * In multi language content the algorithm selects the most compact representation using ECI modes.
 * For example the most compact representation of the string "\u0150\u015C" (O-double-acute, S-circumflex) is
 * ECI(UTF-8), BYTE(\u0150\u015C) while prepending one or more times the same leading character as in
 * "\u0150\u0150\u015C", the most compact representation uses two ECIs so that the string is encoded as
 * ECI(ISO-8859-2), BYTE(\u0150\u0150), ECI(ISO-8859-3), BYTE(\u015C).
 *
 * @author Alex Geller
 */
final class MinimalEncoder {

  // Mike-CHANGED: compacted VersionSize
  private static final int VERSIONS = 40 << 24 | 26 << 16 | 9 << 8 /*| 0*/;

  // List of encoders that potentially encode characters not in ISO-8859-1 in one byte.
  private static final List<CharsetEncoder> ENCODERS = new ArrayList<>();
  static { // Mike-CHANGED encoder search algorithm
    StringBuilder sb = new StringBuilder("ISO-8859-");
    tryAddEncoder(sb, 2, 12);
    tryAddEncoder(sb, 13, 17);
    sb.delete(0, sb.length()).append("windows-125");
    tryAddEncoder(sb, 0, 9);
    tryAddEncoder("Shift_JIS");
  }
  private static void tryAddEncoder(StringBuilder sb, int start, int until) {
    int length = sb.length();
    for (int i = start; i < until; i++) {
      tryAddEncoder(sb.append(i).toString());
      sb.setLength(length);
    }
  }
  private static void tryAddEncoder(String name) {
    if (Encoder.eciByName(name) != null) {
      try {
        ENCODERS.add(Charset.forName(name).newEncoder());
      } catch (UnsupportedCharsetException e) {
        // continue
      }
    }
  }


  // Mike-CHANGED unprivated
  final String stringToEncode;
  final boolean isGS1;
  final CharsetEncoder[] encoders;
  private final int priorityEncoderIndex;
  final ErrorCorrectionLevel ecLevel;
  // END Mike-CHANGED

  /**
   * Creates a MinimalEncoder
   *
   * @param stringToEncode The string to encode
   * @param priorityCharset The preferred {@link Charset}. When the value of the argument is null, the algorithm
   *   chooses charsets that leads to a minimal representation. Otherwise the algorithm will use the priority
   *   charset to encode any character in the input that can be encoded by it if the charset is among the
   *   supported charsets.
   * @param isGS1 {@code true} if a FNC1 is to be prepended; {@code false} otherwise
   * @param ecLevel The error correction level.
   * @see ResultList#getVersion
   */
  MinimalEncoder(String stringToEncode, Charset priorityCharset, boolean isGS1, ErrorCorrectionLevel ecLevel) {

    this.stringToEncode = stringToEncode;
    this.isGS1 = isGS1;
    this.ecLevel = ecLevel;

    List<CharsetEncoder> neededEncoders = new ArrayList<>();
    neededEncoders.add(StandardCharsets.ISO_8859_1.newEncoder());
    boolean needUnicodeEncoder = priorityCharset != null && priorityCharset.name().startsWith("UTF");

    for (int i = 0; i < stringToEncode.length(); i++) {
      boolean canEncode = false;
      for (CharsetEncoder encoder : neededEncoders) {
        if (encoder.canEncode(stringToEncode.charAt(i))) {
          canEncode = true;
          break;
        }
      }

      if (!canEncode) {
        for (CharsetEncoder encoder : ENCODERS) {
          if (encoder.canEncode(stringToEncode.charAt(i))) {
            neededEncoders.add(encoder);
            canEncode = true;
            break;
          }
        }
      }

      if (!canEncode) {
        needUnicodeEncoder = true;
      }
    }

    if (neededEncoders.size() == 1 && !needUnicodeEncoder) {
      encoders = new CharsetEncoder[] { neededEncoders.get(0) };
    } else {
      encoders = new CharsetEncoder[neededEncoders.size() + 2];
      int index = 0;
      for (CharsetEncoder encoder : neededEncoders) {
        encoders[index++] = encoder;
      }

      encoders[index] = StandardCharsets.UTF_8.newEncoder();
      encoders[index + 1] = StandardCharsets.UTF_16BE.newEncoder();
    }

    int priorityEncoderIndexValue = -1;
    if (priorityCharset != null) {
      for (int i = 0; i < encoders.length; i++) {
        if (encoders[i] != null && priorityCharset.name().equals(encoders[i].charset().name())) {
          priorityEncoderIndexValue = i;
          break;
        }
      }
    }
    priorityEncoderIndex = priorityEncoderIndexValue;
  }

  // Mike-REMOVED version, encode

  // Mike-REMOVED version
  List<ResultNode> encode(int[] outVersion) throws WriterException {
    // compute minimal encoding trying the three version sizes.
    List<ResultNode> result = null;
    int smallestSize = Integer.MAX_VALUE;
    int smallestResult = -1;
    for (int i = 0; i < 3; i++) {
      // Mike-CHANGED generation algorithm and added setting outVersion
      int tryVersion = (VERSIONS >>> (8 * (i + 1))) & 0xFF;
      int prevOutVersion = outVersion[0];
      outVersion[0] = tryVersion;
      List<ResultNode> r = encodeSpecificVersion(outVersion);
      int size = getSize(r, outVersion[0]);
      if (Encoder.willFit(size, tryVersion, ecLevel) && size < smallestSize) {
        smallestSize = size;
        smallestResult = i;
        result = r;
      } else {
        outVersion[0] = prevOutVersion;
      }
    }
    if (smallestResult < 0) {
      throw new WriterException("Data too big for any version");
    }
    return result;
  }

  // Mike-CHANGED from returnung enum
  static int getVersionSize(int version) {
    return version <= 9 ? 0 : version <= 26 ? 1 : 2;
  }

  // Mike-CHANGED: inlined isNumeric, isDoubleByteKanji, isAlphanumeric; replaced enum switch with ordinal switch
  boolean canEncode(Mode mode, char c) {
    switch (mode.ordinal()) {
      case 0: return Encoder.isOnlyDoubleByteKanji(String.valueOf(c));
      case 1: return Encoder.getAlphanumericCode(c) != -1;
      case 2: return c >= '0' && c <= '9';
      case 3: return true; // any character can be encoded as byte(s). Up to the caller to manage splitting into
                              // multiple bytes when String.getBytes(Charset) return more than one byte.
      default:
        return false;
    }
  }

  static int getCompactedOrdinal(Mode mode) {
    int ord; // Mike-CHANGED eliminated switch
    if (mode == null) {
      return 0;
    } else if ((ord = mode.ordinal()) > 3) {
      throw new IllegalStateException("Illegal mode " + mode);
    } else {
      return ord;
    }
  }

  void addEdge(List<Edge>[][][] edges, int position, Edge edge) {
    int vertexIndex = position + edge.characterLength;
    if (edges[vertexIndex][edge.charsetEncoderIndex][getCompactedOrdinal(edge.mode)] == null) {
      edges[vertexIndex][edge.charsetEncoderIndex][getCompactedOrdinal(edge.mode)] = new ArrayList<>();
    }
    edges[vertexIndex][edge.charsetEncoderIndex][getCompactedOrdinal(edge.mode)].add(edge);
  }

  void addEdges(int version, List<Edge>[][][] edges, int from, Edge previous) {
    int start = 0;
    int end = encoders.length;
    if (priorityEncoderIndex >= 0 && encoders[priorityEncoderIndex].canEncode(stringToEncode.charAt(from))) {
      start = priorityEncoderIndex;
      end = priorityEncoderIndex + 1;
    }

    for (int i = start; i < end; i++) {
      if (encoders[i].canEncode(stringToEncode.charAt(from))) {
        addEdge(edges, from, new Edge(Mode.BYTE, from, i, 1, previous, version));
      }
    }

    if (canEncode(Mode.KANJI, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(Mode.KANJI, from, 0, 1, previous, version));
    }

    int inputLength = stringToEncode.length();
    if (canEncode(Mode.ALPHANUMERIC, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(Mode.ALPHANUMERIC, from, 0, from + 1 >= inputLength ||
          !canEncode(Mode.ALPHANUMERIC, stringToEncode.charAt(from + 1)) ? 1 : 2, previous, version));
    }

    if (canEncode(Mode.NUMERIC, stringToEncode.charAt(from))) {
      addEdge(edges, from, new Edge(Mode.NUMERIC, from, 0, from + 1 >= inputLength ||
          !canEncode(Mode.NUMERIC, stringToEncode.charAt(from + 1)) ? 1 : from + 2 >= inputLength ||
          !canEncode(Mode.NUMERIC, stringToEncode.charAt(from + 2)) ? 2 : 3, previous, version));
    }
  }
  // Mike-CHANGED to return list without a wrapper; made version parameter inout
  List<ResultNode> encodeSpecificVersion(int[] version) throws WriterException {

    @SuppressWarnings("checkstyle:lineLength")
    /* A vertex represents a tuple of a position in the input, a mode and a character encoding where position 0
     * denotes the position left of the first character, 1 the position left of the second character and so on.
     * Likewise the end vertices are located after the last character at position stringToEncode.length().
     *
     * An edge leading to such a vertex encodes one or more of the characters left of the position that the vertex
     * represents and encodes it in the same encoding and mode as the vertex on which the edge ends. In other words,
     * all edges leading to a particular vertex encode the same characters in the same mode with the same character
     * encoding. They differ only by their source vertices who are all located at i+1 minus the number of encoded
     * characters.
     *
     * The edges leading to a vertex are stored in such a way that there is a fast way to enumerate the edges ending
     * on a particular vertex.
     *
     * The algorithm processes the vertices in order of their position thereby performing the following:
     *
     * For every vertex at position i the algorithm enumerates the edges ending on the vertex and removes all but the
     * shortest from that list.
     * Then it processes the vertices for the position i+1. If i+1 == stringToEncode.length() then the algorithm ends
     * and chooses the the edge with the smallest size from any of the edges leading to vertices at this position.
     * Otherwise the algorithm computes all possible outgoing edges for the vertices at the position i+1
     *
     * Examples:
     * The process is illustrated by showing the graph (edges) after each iteration from left to right over the input:
     * An edge is drawn as follows "(" + fromVertex + ") -- " + encodingMode + "(" + encodedInput + ") (" +
     * accumulatedSize + ") --> (" + toVertex + ")"
     *
     * Example 1 encoding the string "ABCDE":
     * Note: This example assumes that alphanumeric encoding is only possible in multiples of two characters so that
     * the example is both short and showing the principle. In reality this restriction does not exist.
     *
     * Initial situation
     * (initial) -- BYTE(A) (20) --> (1_BYTE)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
     *
     * Situation after adding edges to vertices at position 1
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE)
     *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
     *
     * Situation after adding edges to vertices at position 2
     * (initial) -- BYTE(A) (20) --> (1_BYTE)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC)
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE)
                                   * (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- BYTE(C) (44) --> (3_BYTE)
     *                                                            (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
     *
     * Situation after adding edges to vertices at position 3
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE)
     *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- BYTE(D) (64) --> (4_BYTE)
     *                                                                                                 (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
     *                                                            (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
     *
     * Situation after adding edges to vertices at position 4
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE) -- BYTE(D) (44) --> (4_BYTE)
     *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC) -- BYTE(E) (55) --> (5_BYTE)
     *
     * Situation after adding edges to vertices at position 5
     * (initial) -- BYTE(A) (20) --> (1_BYTE) -- BYTE(B) (28) --> (2_BYTE) -- BYTE(C)         (36) --> (3_BYTE) -- BYTE(D)         (44) --> (4_BYTE) -- BYTE(E)         (52) --> (5_BYTE)
     *                               (1_BYTE) -- ALPHANUMERIC(BC)                             (44) --> (3_ALPHANUMERIC) -- ALPHANUMERIC(DE)                             (55) --> (5_ALPHANUMERIC)
     * (initial) -- ALPHANUMERIC(AB)                     (24) --> (2_ALPHANUMERIC) -- ALPHANUMERIC(CD)                             (35) --> (4_ALPHANUMERIC)
     *
     * Encoding as BYTE(ABCDE) has the smallest size of 52 and is hence chosen. The encodation ALPHANUMERIC(ABCD),
     * BYTE(E) is longer with a size of 55.
     *
     * Example 2 encoding the string "XXYY" where X denotes a character unique to character set ISO-8859-2 and Y a
     * character unique to ISO-8859-3. Both characters encode as double byte in UTF-8:
     *
     * Initial situation
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE)
     *
     * Situation after adding edges to vertices at position 1
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2)
     *                               (1_BYTE_ISO-8859-2) -- BYTE(X) (72) --> (2_BYTE_UTF-8)
     *                               (1_BYTE_ISO-8859-2) -- BYTE(X) (72) --> (2_BYTE_UTF-16BE)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE)
     *
     * Situation after adding edges to vertices at position 2
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2)
     *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3)
     *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (80) --> (3_BYTE_UTF-8)
     *                                                                       (2_BYTE_ISO-8859-2) -- BYTE(Y) (80) --> (3_BYTE_UTF-16BE)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE)
     *
     * Situation after adding edges to vertices at position 3
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3)
     *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (80) --> (4_BYTE_ISO-8859-3)
     *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (112) --> (4_BYTE_UTF-8)
     *                                                                                                               (3_BYTE_ISO-8859-3) -- BYTE(Y) (112) --> (4_BYTE_UTF-16BE)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8) -- BYTE(Y) (72) --> (3_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE) -- BYTE(Y) (72) --> (3_BYTE_UTF-16BE)
     *
     * Situation after adding edges to vertices at position 4
     * (initial) -- BYTE(X) (32) --> (1_BYTE_ISO-8859-2) -- BYTE(X) (40) --> (2_BYTE_ISO-8859-2) -- BYTE(Y) (72) --> (3_BYTE_ISO-8859-3) -- BYTE(Y) (80) --> (4_BYTE_ISO-8859-3)
     *                                                                                                               (3_BYTE_UTF-8) -- BYTE(Y) (88) --> (4_BYTE_UTF-8)
     *                                                                                                               (3_BYTE_UTF-16BE) -- BYTE(Y) (88) --> (4_BYTE_UTF-16BE)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-8) -- BYTE(X) (56) --> (2_BYTE_UTF-8) -- BYTE(Y) (72) --> (3_BYTE_UTF-8)
     * (initial) -- BYTE(X) (40) --> (1_BYTE_UTF-16BE) -- BYTE(X) (56) --> (2_BYTE_UTF-16BE) -- BYTE(Y) (72) --> (3_BYTE_UTF-16BE)
     *
     * Encoding as ECI(ISO-8859-2),BYTE(XX),ECI(ISO-8859-3),BYTE(YY) has the smallest size of 80 and is hence chosen.
     * The encodation ECI(UTF-8),BYTE(XXYY) is longer with a size of 88.
     */

    int inputLength = stringToEncode.length();

    // Array that represents vertices. There is a vertex for every character, encoding and mode. The vertex contains
    // a list of all edges that lead to it that have the same encoding and mode.
    // The lists are created lazily

    // The last dimension in the array below encodes the 4 modes KANJI, ALPHANUMERIC, NUMERIC and BYTE via the
    // function getCompactedOrdinal(Mode)
    @SuppressWarnings("unchecked")
    List<Edge>[][][] edges = new ArrayList[inputLength + 1][encoders.length][4];
    int v = version[0];
    addEdges(v, edges, 0, null);

    for (int i = 1; i <= inputLength; i++) {
      for (int j = 0; j < encoders.length; j++) {
        for (int k = 0; k < 4; k++) {
          Edge minimalEdge;
          if (edges[i][j][k] != null) {
            List<Edge> localEdges = edges[i][j][k];
            int minimalIndex = -1;
            int minimalSize = Integer.MAX_VALUE;
            for (int l = 0; l < localEdges.size(); l++) {
              Edge edge = localEdges.get(l);
              if (edge.cachedTotalSize < minimalSize) {
                minimalIndex = l;
                minimalSize = edge.cachedTotalSize;
              }
            }
            assert minimalIndex != -1;
            minimalEdge = localEdges.get(minimalIndex);
            localEdges.clear();
            localEdges.add(minimalEdge);
            if (i < inputLength) {
              addEdges(v, edges, i, minimalEdge);
            }
          }
        }
      }

    }
    int minimalJ = -1;
    int minimalK = -1;
    int minimalSize = Integer.MAX_VALUE;
    for (int j = 0; j < encoders.length; j++) {
      for (int k = 0; k < 4; k++) {
        if (edges[inputLength][j][k] != null) {
          List<Edge> localEdges = edges[inputLength][j][k];
          assert localEdges.size() == 1;
          Edge edge = localEdges.get(0);
          if (edge.cachedTotalSize < minimalSize) {
            minimalSize = edge.cachedTotalSize;
            minimalJ = j;
            minimalK = k;
          }
        }
      }
    }
    if (minimalJ < 0) {
      throw new WriterException("Internal error: failed to encode \"" + stringToEncode + "\"");
    }
    return ResultList(version, edges[inputLength][minimalJ][minimalK].get(0), isGS1, ecLevel, encoders, stringToEncode);
  }

  private final class Edge { // Mike-CHANGED visibilities to package-private to avoid synthetic accessor calls
    final Mode mode;
    final int fromPosition;
    final int charsetEncoderIndex;
    final int characterLength;
    final Edge previous;
    final int cachedTotalSize;

    Edge(Mode mode, int fromPosition, int charsetEncoderIndex, int characterLength, Edge previous, int version) {
      this.mode = mode;
      this.fromPosition = fromPosition;
      this.charsetEncoderIndex = mode == Mode.BYTE || previous == null ? charsetEncoderIndex :
          previous.charsetEncoderIndex; // inherit the encoding if not of type BYTE
      this.characterLength = characterLength;
      this.previous = previous;

      int size = previous != null ? previous.cachedTotalSize : 0;

      boolean needECI = mode == Mode.BYTE &&
          (previous == null && this.charsetEncoderIndex != 0) || // at the beginning and charset is not ISO-8859-1
          (previous != null && this.charsetEncoderIndex != previous.charsetEncoderIndex);

      if (previous == null || mode != previous.mode || needECI) {
        size += 4 + mode.getCharacterCountBits(version);
      }
      switch (mode.ordinal()) { // Mike-CHANGED: int switch instead of enum one
        case 0:
          size += 13;
          break;
        case 1:
          size += characterLength == 1 ? 6 : 11;
          break;
        case 2:
          size += characterLength == 1 ? 4 : characterLength == 2 ? 7 : 10;
          break;
        case 3: // Mike-CHANGED outlined method
          size += 8 * uglyFuckingByteCount(stringToEncode, encoders[charsetEncoderIndex], fromPosition, characterLength);
          if (needECI) {
            size += 4 + 8; // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
          }
          break;
      }
      cachedTotalSize = size;
    }
  }

  // Mike-ADDED
  static int uglyFuckingByteCount(String stringToEncode, CharsetEncoder encoder, int fromPosition, int characterLength) {
    return stringToEncode.substring(fromPosition, fromPosition + characterLength).getBytes(encoder.charset()).length;
  }

  // Mike-CHANGED replaced class with a method
  List<ResultNode> ResultList(int[] version, Edge solution, boolean isGS1, ErrorCorrectionLevel ecLevel, CharsetEncoder[] encoders, String stringToEncode) {
    int length = 0;
    Edge current = solution;
    boolean containsECI = false;

    List<ResultNode> list = new ArrayList<>();
    while (current != null) {
      length += current.characterLength;
      Edge previous = current.previous;

      boolean needECI = current.mode == Mode.BYTE &&
          (previous == null && current.charsetEncoderIndex != 0) || // at the beginning and charset is not ISO-8859-1
          (previous != null && current.charsetEncoderIndex != previous.charsetEncoderIndex);

      if (needECI) {
        containsECI = true;
      }

      if (previous == null || previous.mode != current.mode || needECI) {
        list.add(0, new ResultNode(current.mode, current.fromPosition, encoders[current.charsetEncoderIndex], length, stringToEncode));
        length = 0;
      }

      if (needECI) {
        list.add(0, new ResultNode(Mode.ECI, current.fromPosition, encoders[current.charsetEncoderIndex], 0, stringToEncode));
      }
      current = previous;
    }

    // prepend FNC1 if needed. If the bits contain an ECI then the FNC1 must be preceeded by an ECI.
    // If there is no ECI at the beginning then we put an ECI to the default charset (ISO-8859-1)
    if (isGS1) {
      ResultNode first = list.get(0);
      if (first != null && first.mode != Mode.ECI && containsECI) {
        // prepend a default character set ECI
        list.add(0, new ResultNode(Mode.ECI, 0, null, 0, stringToEncode));
      }
      first = list.get(0);
      // prepend or insert a FNC1_FIRST_POSITION after the ECI (if any)
      list.add(first.mode != Mode.ECI ? 0 : 1, new ResultNode(Mode.FNC1_FIRST_POSITION, 0, null, 0, stringToEncode));
    }

    // set version to smallest version into which the bits fit.
    int v = version[0]; // Mike-CHANGED version determining algorithm
    int vSize = getVersionSize(v);
    int size = getSize(list, v);
    // increase version if needed
    int upperLimit = (VERSIONS >>> (8 * (vSize + 1))) & 0xFF;
    while (v < upperLimit && !Encoder.willFit(size, v, ecLevel)) v++;
    // shrink version if possible
    int lowerLimit = (VERSIONS >>> (8 * (vSize + 1)) & 0xFF);
    while (v > lowerLimit && Encoder.willFit(size, v - 1, ecLevel)) v--;
    version[0] = v;
    return list;
  }

  // Mike-REMOVED getSize()
  int getSize(List<ResultNode> list, int version) { // Mike-CHANGED parameters
    int result = 0;
    for (ResultNode resultNode : list) {
      result += resultNode.getSize(version);
    }
    return result;
  }

  /**
   * appends the bits
   */
  void getBits(List<ResultNode> list, BitArray bits, int version) throws WriterException {
    for (ResultNode resultNode : list) { // Mike-CHANGED parameters
      resultNode.getBits(bits, version);
    }
  }

  // Mike-INLINED getVersion()
  // Mike-REMOVED toString()

  static final class ResultNode {  // Mike-CHANGED parameters, made static, unprivated mode

    // Mike-CHANGED visibility to package-private
    final Mode mode;
    final int fromPosition;
    final CharsetEncoder encoder;
    final int characterLength;
    final String stringToEncode;
    // END Mike-CHANGED

    ResultNode(Mode mode, int fromPosition, CharsetEncoder encoder, int characterLength, String stringToEncode) {
      this.mode = mode;
      this.fromPosition = fromPosition;
      this.encoder = encoder;
      this.characterLength = characterLength;
      this.stringToEncode = stringToEncode;
    }

    /**
     * returns the size in bits
     */
    int getSize(int version) { // Mike-CHANGED visibility to package-private
      int size = 4 + mode.getCharacterCountBits(version);
      switch (mode.ordinal()) { // Mike-CHANGED: replaced enum switch with int one
        case 0:
          size += 13 * characterLength;
          break;
        case 1:
          size += (characterLength / 2) * 11;
          size += (characterLength % 2) == 1 ? 6 : 0;
          break;
        case 2:
          size += (characterLength / 3) * 10;
          int rest = characterLength % 3;
          size += rest == 1 ? 4 : rest == 2 ? 7 : 0;
          break;
        case 3:
          size += 8 * getCharacterCountIndicator();
          break;
        case 4:
          size += 8; // the ECI assignment numbers for ISO-8859-x, UTF-8 and UTF-16 are all 8 bit long
      }
      return size;
    }

    /**
     * returns the length in characters according to the specification (differs from getCharacterLength() in BYTE mode
     * for multi byte encoded characters)
     */
    private int getCharacterCountIndicator() { // Mike-CHANGED outlined method
      return mode == Mode.BYTE ? uglyFuckingByteCount(stringToEncode, encoder, fromPosition, characterLength) : characterLength;
    }

    /**
     * appends the bits
     */
    void getBits(BitArray bits, int version) throws WriterException { // Mike-CHANGED visibility to package-private
      bits.appendBits(mode.getBits(), 4);
      if (characterLength > 0) {
        int length = getCharacterCountIndicator();
        bits.appendBits(length, mode.getCharacterCountBits(version));
      }
      if (mode == Mode.ECI) {
        bits.appendBits(Encoder.eciByName(encoder.charset().name()), 8);
      } else if (characterLength > 0) {
        // append data
        Encoder.appendBytes(stringToEncode, fromPosition, fromPosition + characterLength, mode, bits, encoder.charset());
      }
    }
    // Mike-REMOVED toString()
  }
}
