package org.jmhplayground.jmh1;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
public class DeadCodeElimination3 {

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

    @Benchmark
    public void notInlinedComputation() {
        notInlinedCompute(x);
    }

    private int compute(int loopNr) {
        int acc = 0;
        for (int i = 0; i < loopNr; i++) {
            acc += i;
        }
        return acc;
    }

    /**
     * Preventing inlining makes the method opaque to the JVM.
     * The JVM cannot perform dead code elimination if it can ensure that the
     * removed code doesn't perform any side effect (like throwing an exception)
     *
     * We can always verify if the method has been actually executed by using 2 separate profilers:
     * 1. -prof perfnorm : if the instruction counter to the one of returnComputation is likely being executed
     * 2. -prof perfasm : the assembly never lies
     */
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int notInlinedCompute(int loopNr) {
        int acc = 0;
        for (int i = 0; i < loopNr; i++) {
            acc += i;
        }
        return acc;
    }
}
