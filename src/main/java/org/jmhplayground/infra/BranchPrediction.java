package org.jmhplayground.infra;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 2)
public class BranchPrediction {

    /**
     * jvmArgsAppend = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining", "-XX:-TieredCompilation", "-XX:InlineSmallCode=4000", "-XX:MaxInlineSize=60"}
     * -rf json -psame=false -pshuffle=true
     * -prof "async:output=flamegraph;dir=/tmp;libPath=/home/mario/software/async-profiler-2.8.1-linux-x64/build/libasyncProfiler.so"
     */

    private static final Set<String> IMMUTABLE_PSEUDO_HEADERS = Set.of(":path", ":authority", ":method", ":status", ":scheme", ":protocol");

    private static final Set<String> HASHSET_PSEUDO_HEADERS = new HashSet<>(List.of(":path", ":authority", ":method", ":status", ":scheme", ":protocol"));

    @Param({"false", "true"})
    private boolean shuffle;

    @Param({"false", "true"})
    private boolean same;

    private String[] inputs;

    @Setup
    public void setup() {
        if (shuffle) {
            if (same) {
                inputs = IMMUTABLE_PSEUDO_HEADERS.toArray(new String[0]);
            } else {
                inputs = IMMUTABLE_PSEUDO_HEADERS.stream().map(String::toCharArray).map(String::new).toArray(count -> new String[count]);
            }
        } else {
            inputs = new String[IMMUTABLE_PSEUDO_HEADERS.size()];
            if (same) {
                Arrays.fill(inputs, ":protocol");
            } else {
                Arrays.fill(inputs, new String(":protocol".toCharArray()));
            }
        }
    }


    private String next() {
        var inputs = this.inputs;
        return inputs[ThreadLocalRandom.current().nextInt(0, inputs.length)];

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
