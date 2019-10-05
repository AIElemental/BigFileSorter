#!/usr/bin/env bash
javac src/ru/aielemental/tests/InplaceFileSorter.java
javac src/ru/aielemental/tests/FileGenerator.java
cd src
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test1.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test2.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test3.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test4.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test5.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test6.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test7.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test8.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test9.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test10.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test11.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test12.txt 16
java -Xmx32m ru.aielemental.tests.InplaceFileSorter ../test13.txt 16

java ru.aielemental.tests.FileGenerator 3 64000000
java -Xmx32m ru.aielemental.tests.InplaceFileSorter generated.txt 65536
du -h generated.txt


