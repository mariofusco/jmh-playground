package org.jmhplayground.jmh1;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
public class DeadCodeElimination2 {

    @Param({"1000", "100000"})
    private int x;

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
