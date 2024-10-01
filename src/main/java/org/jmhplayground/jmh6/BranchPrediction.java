package org.jmhplayground.jmh6;

import java.util.*;
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
 * jvmArgsAppend = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining", "-XX:-TieredCompilation", "-XX:InlineSmallCode=4000", "-XX:MaxInlineSize=60"}
 *
 * Run with
 * -rf json -psame=false -pshuffle=true -prof perfnorm -prof "async:output=flamegraph;dir=/tmp;libPath=/home/mario/software/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 2)
public class BranchPrediction {

    private static final Set<String> IMMUTABLE_PSEUDO_HEADERS = Set.of(":path", ":authority", ":method", ":status", ":scheme", ":protocol");

    private static final Set<String> HASHSET_PSEUDO_HEADERS = new HashSet<>(List.of(":path", ":authority", ":method", ":status", ":scheme", ":protocol"));

    @Param({"false", "true"})
    private boolean shuffle;

    @Param({"false", "true"})
    private boolean same;

    private String[] inputs;
    // keep it as small as possible to have more and more of them in the L1 cache
    private byte[] inputsSequence;

    private long nextSequence;

    @Setup
    public void setup() {
        // let's use a power of 2 here for convenience
        int sequenceSize = 128 * 1024;
        if (Integer.bitCount(sequenceSize) != 1) {
            throw new IllegalArgumentException("sequenceSize must be a power of 2");
        }
        inputsSequence = new byte[sequenceSize];
        if (same) {
            inputs = IMMUTABLE_PSEUDO_HEADERS.toArray(new String[0]);
        } else {
            inputs = IMMUTABLE_PSEUDO_HEADERS.stream().map(String::toCharArray).map(String::new).toArray(count -> new String[count]);
        }
        if (shuffle) {
            var rnd = new Random(42);
            for (int i = 0; i < sequenceSize; i++) {
                inputsSequence[i] = (byte) rnd.nextInt(0, IMMUTABLE_PSEUDO_HEADERS.size());
            }
        } else {
            // this should be fairly predictable for the CPU :P
            for (int i = 0; i < sequenceSize; i++) {
                inputsSequence[i] = (byte) (i % IMMUTABLE_PSEUDO_HEADERS.size());
            }
        }
    }


    private String next() {
        var inputsSequence = this.inputsSequence;
        int nextSequenceIndex = (int) (nextSequence & (inputsSequence.length - 1));
        int nextInputIndex = inputsSequence[nextSequenceIndex];
        nextSequence++;
        return inputs[nextInputIndex];
    }


    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean immutableSet() {
        return IMMUTABLE_PSEUDO_HEADERS.contains(next());
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean hashSet() {
        return HASHSET_PSEUDO_HEADERS.contains(next());
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean stringSwitch() {

        return switch (next()) {
            case ":path", ":authority", ":method", ":status", ":scheme", ":protocol" -> true;
            default -> false;
        };
    }
}
