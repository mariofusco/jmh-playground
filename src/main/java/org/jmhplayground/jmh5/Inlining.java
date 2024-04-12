package org.jmhplayground.jmh5;

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

/**
 * Beware results without inspecting first the inlining decision performed by just-in-time
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = "-XX:MaxInlineLevel=4")
public class Inlining {

    private static class AsciiString implements CharSequence {
        private final byte[] ascii;

        private AsciiString(CharSequence ascii) {
            this(ascii.toString().getBytes(StandardCharsets.US_ASCII));
        }

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

    @Param({"0", "1", "2", "3", "4", "5"})
    int callDepth;

    AsciiString uppercase;

    @Setup
    public void setup() {
        if (callDepth < 0 || callDepth > 5) {
            throw new UnsupportedOperationException();
        }
        var sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append('A');
        }
        uppercase = new AsciiString(sb);

    }

    @Benchmark
    public boolean isUppercase() {
        if (callDepth == 0) {
            return checkUppercase(uppercase);
        }
        return isUp1(uppercase);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean controlledIsUppercase() {
        if (callDepth == 0) {
            return checkUppercase(uppercase);
        }
        return isUp1(uppercase);
    }

    private boolean isUp1(CharSequence in) {
        if (callDepth == 1) {
            return checkUppercase(in);
        }
        return isUp2(in);
    }

    private boolean isUp2(CharSequence in) {
        if (callDepth == 2) {
            return checkUppercase(in);
        }
        return isUp3(in);
    }

    private boolean isUp3(CharSequence in) {
        if (callDepth == 3) {
            return checkUppercase(in);
        }
        return isUp4(in);
    }

    private boolean isUp4(CharSequence in) {
        if (callDepth == 4) {
            return checkUppercase(in);
        }
        return isUp5(in);
    }

    private boolean isUp5(CharSequence in) {
        return checkUppercase(in);
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
