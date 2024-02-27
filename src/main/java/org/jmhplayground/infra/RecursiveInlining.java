package org.jmhplayground.infra;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining", "-XX:MaxInlineLevel=4"})
public class RecursiveInlining {

    /**
     *
     */

    private static class AsciiString implements CharSequence {
        private final byte[] ascii;

        private AsciiString(byte[] ascii) {
            this.ascii = ascii;
        }

        @Override
        public int length() {
            return ascii.length;
        }

        @Override
        public char charAt(int index) {
            return (char) ascii[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
        }
    }

    @Param("100")
    int size;

    @Param({"0","4", "8"})
    int callDepth;

    AsciiString uppercase;

    @Setup
    public void setup() {
        var sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append('A');
        }
        uppercase = new AsciiString(sb.toString().getBytes(StandardCharsets.US_ASCII));

    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean isUppercase() {
        int level = callDepth;
        if (level == 0) {
            return checkUppercase(uppercase);
        }
        return isUp1(uppercase, level-1);
    }

    private boolean isUp1(CharSequence in, int level) {
        if (level == 0) {
            return checkUppercase(uppercase);
        }
        return isUp2(in, level-1);
    }

    private boolean isUp2(CharSequence in, int level) {
        if (level == 0) {
            return checkUppercase(uppercase);
        }
        return isUp1(in, level-1);
    }

    private boolean checkUppercase(CharSequence in) {
        for (int i = 0; i < in.length(); i++) {
            char ch = in.charAt(i);
            if (!(ch >= 'A' && ch <= 'Z')) {
                return false;
            }
        }
        return true;
    }


}
