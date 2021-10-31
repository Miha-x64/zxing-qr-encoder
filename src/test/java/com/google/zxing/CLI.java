package com.google.zxing;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * Toy CLI encoder.
 */
public final class CLI {
    private CLI() {}

    private static final String[] ECL_NAMES =
        stream(ErrorCorrectionLevel.values()).map(Enum::name).toArray(String[]::new);

    private static final String SQUARE = "██";
    private static final String[] ALPHABET = { "\u001b[37m", "\u001b[30m", "\u001b[0m" };
    //                                          white         black         reset

    public static void main(String[] args) throws IOException, WriterException {
        try {
            work(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void work(String[] args) throws IOException, WriterException {
        Boolean inBinaryOutput = null;
        ErrorCorrectionLevel inLevel = null;
        String input = null;
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg.equals("--help")) {
                    System.out.println("Usage: [-o=text|binary] [-i] [-l=" + join("|", ECL_NAMES) + "] [data]");
                    System.out.println("If no “data” argument, reads stdin until EOF or EOT (Ctrl+D).");
                    return;
                } else if (arg.startsWith("-o=")) {
                    if (inBinaryOutput != null)
                        throw new IllegalArgumentException("Output format is specified more than once.");
                    switch (args[0].substring(3).toLowerCase(Locale.ROOT)) {
                        case "text": inBinaryOutput = false; break;
                        case "binary": inBinaryOutput = true; break;
                        default: throw new IllegalArgumentException("-o must be either “text” or “binary”.");
                    }
                } else if (arg.startsWith("-l=")) {
                    if (inBinaryOutput != null)
                        throw new IllegalArgumentException("Error correction level is specified more than once.");
                    try {
                        inLevel = ErrorCorrectionLevel.valueOf(arg.substring(3));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("-l must be one of " +
                            stream(ECL_NAMES).map(l -> '“' + l + '”').collect(joining(", ")));
                    }
                } else {
                    if (input != null)
                        throw new IllegalArgumentException("Found more than one input.");
                    input = arg;
                }
            }
        }

        boolean binaryOutput = inBinaryOutput != null && inBinaryOutput;
        ErrorCorrectionLevel level = inLevel == null ? ErrorCorrectionLevel.L : inLevel;

        ByteArrayOutputStream stream;
        boolean interactive = input == null && System.console() != null; // just skip getting Console if have input
        if (interactive) {
            ByteArrayOutputStream finalStream = stream = new ByteArrayOutputStream();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    String lastInput = finalStream.toString().trim();
                    if (!lastInput.isEmpty()) {
                        System.out.println(); // fix ^C in console
                        spit(binaryOutput, level, lastInput);
                    }
                } catch (WriterException e) {
                    throw new RuntimeException(e);
                }
            }));
        } else {
            stream = null;
        }
        //noinspection LoopConditionNotUpdatedInsideLoop — as designed
        do {
            if (input == null) {
                if (interactive)
                    System.out.println("Enter data line by line, then send EOF (Ctrl+D) or SIGINT (Ctrl+C).");
                if (stream == null) stream = new ByteArrayOutputStream();
                int c;
                while ((c = System.in.read()) > 0) stream.write(c);
                input = stream.toString().trim(); // eat \n, LOL
                stream.reset();
            }
            spit(binaryOutput, level, input);
            input = null;
        } while (interactive);
    }

    static void spit(boolean binaryOutput, ErrorCorrectionLevel level, String input) throws WriterException {
        QRCode out = Encoder.encode(input, level);
        ByteMatrix matrix = out.matrix;
        if (binaryOutput) {
            System.out.write(out.version);
            int currentByte = 0;
            int writtenBits = 0;
            for (int y = 0; y < matrix.height; y++) {
                for (int x = 0; x < matrix.width; x++) {
                    currentByte = (currentByte << 1) | matrix.get(x, y);
                    if (++writtenBits == 8) {
                        System.out.write(currentByte);
                        writtenBits = currentByte = 0;
                    }
                }
            }
        } else {
            printHBorder(matrix.width);
            printHBorder(matrix.width);
            for (int y = 0; y < matrix.height; y++) {
                printSquare(2, 0);
                printSquare(0, 0);
                int prev = 0;
                for (int x = 0; x < matrix.width; x++)
                    printSquare(prev, prev = matrix.get(x, y));
                printSquare(prev, 0);
                printSquare(0, 0);
                System.out.println();
            }
            printHBorder(matrix.width);
            printHBorder(matrix.width);
            System.out.println(ALPHABET[2]);
        }
        System.out.flush();
    }

    private static void printHBorder(int mWidth) {
        System.out.print(ALPHABET[0]);
        for (int i = 0; i < mWidth + 4; i++) System.out.print(SQUARE);
        System.out.println();
    }

    private static void printSquare(int prev, int curr) {
        if (prev != curr) System.out.print(ALPHABET[curr]);
        System.out.print(SQUARE);
    }
}
