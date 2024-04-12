package org.jmhplayground.jmh1;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
public class DeadCodeElimination2 {

    private int x;

    @Setup
    public void setup() {
        x = 1000;
    }

    @Benchmark
    public void measureComputation() {
        compute(x);
    }

    @Benchmark
    public int returnComputation() {
        return compute(x);
    }

    private int compute(int loopNr) {
        int acc = 0;
        for (int i = 0; i < loopNr; i++) {
            acc += i;
        }
        return acc;
    }
}
