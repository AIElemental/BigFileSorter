package ru.aielemental.tests;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author Artem Bulanov <aite@yandex-team.ru>
 * Created at 2019-10-05
 */
public class FileGenerator {

    static final int BLOCK = 1024 * 64;
    static final char[] CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Can pass 2 args: line count, and line length");
        }
        Random rnd = new Random();
        int lineCount = 100;
        if (args.length >= 1) {
            lineCount = Integer.parseInt(args[0]);
        }
        int lineSize = 100;
        if (args.length >= 2) {
            lineSize = Integer.parseInt(args[1]);
        }
        int lineBulks = lineSize / BLOCK;
        int lineTail = lineSize % BLOCK;

        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
        Date date = new Date();
//        RandomAccessFile file = new RandomAccessFile("generated_" + dateFormat.format(date) + ".txt", "rw");
        RandomAccessFile file = new RandomAccessFile("generated.txt", "rw");
        for (int i = 0; i < lineCount; i++) {
            for (int j = 0; j < lineBulks; j++) {
                StringBuilder a = new StringBuilder();
                for (int k = 0; k < BLOCK; k++) {
                    a.append(CHARS[Math.abs(rnd.nextInt()) % CHARS.length]);
                }
                if (a.length() > 0) {
                    file.write(a.toString().getBytes());
                }
            }
            StringBuilder a = new StringBuilder();
            for (int j = 0; j < lineTail; j++) {
                a.append(CHARS[Math.abs(rnd.nextInt()) % CHARS.length]);
            }
            if (a.length() > 0) {
                file.write(a.toString().getBytes());
            }
            file.write('\n');
        }
    }
}
