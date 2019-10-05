package ru.aielemental.tests;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Optional;

public class InplaceFileSorter {
    long bytesRead = 0;
    long bytesWritten = 0;

    final String filePath;
    final int bufSize;
    final byte[] line1Buf;
    final byte[] line2Buf;

    public InplaceFileSorter(String filePath, int bufSize) {
        this.filePath = filePath;
        this.bufSize = bufSize;
        line1Buf = new byte[bufSize];
        line2Buf = new byte[bufSize];
    }

    public static void main(String[] args) throws IOException {
        String filename = "generated.txt";
        if (args.length >= 1) {
            filename = args[0];
        }
        System.out.println("Reading " + filename);
        int bufSize = 32;
        if (args.length >= 2) {
            bufSize = Integer.parseInt(args[1]);
        }
        InplaceFileSorter sorter = new InplaceFileSorter(filename, bufSize);
        long elapsedMillis = -System.currentTimeMillis();
        sorter.sort();
        elapsedMillis += System.currentTimeMillis();
        System.out.println("Millis sorting " + elapsedMillis + "ms");
        System.out.println("Sorting Done for " + filename);
        try (RandomAccessFile file = new RandomAccessFile(filename, "rw")) {
            long fileLength = file.length();
            System.out.println("File size " + fileLength);
            if (fileLength > 0) {
                System.out.println("Read bytes " + sorter.bytesRead + " " + sorter.bytesRead / fileLength + " full files");
                System.out.println("Written bytes " + sorter.bytesWritten + " " + sorter.bytesWritten / fileLength + " full files");
            }
        }
    }

    public void sort() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "rw")) {
            boolean changed = true;
            while (changed) {
                changed = false;

                long nextLineToCheck = 2;
                long currentLineStartIndex = 0;
                long currentLineLength;
                long nextLineLength;
                Optional<Long> nextNewLineO = findNextNewLine(file, 0);
                if (nextNewLineO.isEmpty()) return;
                currentLineLength = nextNewLineO.get();
                Optional<Long> nextLineEndIndexO = findNextLineEnd(file, currentLineStartIndex + currentLineLength + 1); //+1 for \n

                while (nextLineEndIndexO.isPresent()) {
                    System.out.println("Checking line:" + nextLineToCheck);
                    long nextLineStart = currentLineStartIndex + currentLineLength + 1; //+1 for \n
                    nextLineLength = nextLineEndIndexO.get() - nextLineStart;
                    boolean needSwap = isLine2LessThanLine1(file,
                            currentLineStartIndex,
                            currentLineLength,
                            nextLineStart,
                            nextLineLength);
                    if (needSwap) {
                        swapLines(file, currentLineStartIndex, currentLineStartIndex + currentLineLength, nextLineLength);
                        changed = true;
                    }
                    currentLineStartIndex = nextLineStart;
                    currentLineLength = nextLineLength;
                    nextLineEndIndexO = findNextLineEnd(file, currentLineStartIndex + currentLineLength + 1);
                    nextLineToCheck++;
                }
            }
        }
    }

    Optional<Long> findNextLineEnd(RandomAccessFile file, long startFrom) throws IOException {
        //new line is next line end
        Optional<Long> newLineSymbolO = findNextNewLine(file, startFrom);
        if (newLineSymbolO.isPresent()) {
            return newLineSymbolO;
        } else if (startFrom < file.length()) {
            //future eof can be last line line end
            return Optional.of(file.length());
        } else {
            return Optional.empty();
        }
    }

    Optional<Long> findNextNewLine(RandomAccessFile file, long startFrom) throws IOException {
        long nextSeekFrom = startFrom;
        long length = file.length();
        while (nextSeekFrom < length) {
            file.seek(nextSeekFrom);
            int read = file.read(line1Buf);
            bytesRead += read;
            if (read == -1) { //eof
                return Optional.empty();
            }
            for (int i = 0; i < read; i++) {
                if (line1Buf[i] == '\n') {
                    return Optional.of(nextSeekFrom + i);
                }
            }
            nextSeekFrom += read;
        }
        return Optional.empty();
    }

    boolean isLine2LessThanLine1(RandomAccessFile file, long line1Start, long line1Length, long line2Start, long line2Length) throws IOException {
        long line1ReadPosition = line1Start;
        long line2ReadPosition = line2Start;

        long fullBufsAvailable = Math.min(line1Length, line2Length) / bufSize;
        for (int i = 0; i < fullBufsAvailable; i++, line1ReadPosition += bufSize, line2ReadPosition += bufSize) {
            int cmp = compareBlocks(file, line1ReadPosition, line2ReadPosition, bufSize);
            if (cmp != 0) {
                return cmp > 0;
            }
        }
        //full blocks were all equal to each other, will read last incomplete BLOCK if present
        int tailSize = (int) (Math.min(line1Length, line2Length) % bufSize);
        if (tailSize > 0) {
            int cmp = compareBlocks(file, line1ReadPosition, line2ReadPosition, tailSize);
            if (cmp != 0) {
                return cmp > 0;
            }
        }
        //common data is all equal, if next line is shorter than current, then true
        return line2Length < line1Length;
    }

    private int compareBlocks(RandomAccessFile file, long line1ReadPosition, long line2ReadPosition, int blockSize) throws IOException {
        file.seek(line1ReadPosition);
        readData(file, line1Buf, blockSize);
        bytesRead += blockSize;
        file.seek(line2ReadPosition);
        readData(file, line2Buf, blockSize);
        bytesRead += blockSize;
        for (int bufIdx = 0; bufIdx < blockSize; bufIdx++) {
            if (line1Buf[bufIdx] > line2Buf[bufIdx]) {
                return +1;
            } else if (line1Buf[bufIdx] < line2Buf[bufIdx]) {
                return -1;
            } //else continue; equals
        }
        return 0;
    }

    //Swap 2 unequally sized byte blocks in file. Blocks must be adjacent with 1 byte separator between them
    void swapLines(RandomAccessFile file, long line1Start, long newLineIdx, long line2Length) throws IOException {
        //обмен строк делим на 3 этапа
        long line2Start = newLineIdx + 1;
        long line1Length = newLineIdx - line1Start;
        if (line1Length > line2Length) {
            //abcdefgh\123
            swapSimple(file, line1Start, line2Start, line2Length);
            //123defgh\abc
            swapAdjacent(file, line1Start + line2Length, line2Start - 1, line2Length + 1);
            //12345678\aaa
        } else if (line1Length < line2Length) {
            //aaa\12345678
            swapSimple(file, line1Start, line2Start + line2Length - line1Length, line1Length);
            //678\12345aaa
            swapAdjacent(file, line1Start, line1Start + line1Length + 1, line2Length - line1Length);
            //12345678\aaa
        } else { //equal case, simple
            //abcd\1234
            swapSimple(file, line1Start, line2Start, line1Length);
            //1234\abcd
        }
    }

    //Swap 2 unequally sized byte blocks in file. Blocks must be adjacent
    private void swapAdjacent(RandomAccessFile file, long line1Start, long line2Start, long line2Length) throws IOException {
        long line1Length = line2Start - line1Start;
        if (line1Length == 0 || line2Length == 0) return;
        if (line1Length > line2Length) {
            //12345aaa
            swapSimple(file, line1Start + line1Length - line2Length, line1Start + line1Length, line2Length);
            //12aaa345
            swapAdjacent(file, line1Start, line2Start - line2Length, line2Length);
        } else if (line1Length < line2Length) {
            //aaa12345
            swapSimple(file, line1Start, line2Start, line1Length);
            //123aaa45
            swapAdjacent(file, line2Start, line2Start + line1Length, line2Length - line1Length);
        } else { //equal case, simple
            //aaaabbbb
            swapSimple(file, line1Start, line2Start, line1Length);
        }
    }

    //Swap 2 equally sized byte blocks in file.
    private void swapSimple(RandomAccessFile file, long line1Start, long line2Start, long length) throws IOException {
        if (length == 0) return;
        long swapped = 0;
        int readLimit;
        while (swapped < length) {
            readLimit = (int) Math.min(bufSize, length - swapped);
            file.seek(line1Start + swapped);
            readData(file, line1Buf, readLimit);
            file.seek(line2Start + swapped);
            readData(file, line2Buf, readLimit);

            file.seek(line2Start + swapped);
            writeData(file, line1Buf, readLimit);
            file.seek(line1Start + swapped);
            writeData(file, line2Buf, readLimit);
            swapped += readLimit;
        }
    }

    //wrapper with counter
    private void readData(RandomAccessFile file, byte[] buf, int length) throws IOException {
        file.readFully(buf, 0, length);
        bytesRead += length;
    }

    //wrapper with counter
    private void writeData(RandomAccessFile file, byte[] buf, int length) throws IOException {
        file.write(buf, 0, length);
        bytesWritten += length;
    }
}
