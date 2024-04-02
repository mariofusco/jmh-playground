package org.jmhplayground;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Not all blackholes are made equals!
 * see https://bugs.openjdk.org/browse/JDK-8259316
 * JDK-17 introduced compiler assisted blackholes which costs and effects on the code is dramatically different
 * from software ones.
 *
 * java -Djmh.blackhole.autoDetect=false -jar target/benchmark.jar org.jmhplayground.JMH2_BlackholeTaxonomy.*
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
public class JMH2_BlackholeTaxonomy {

    @Param({"100"})
    private int loopCount;

    @Benchmark
    public void blackholeLoop(Blackhole bh) {
        for (int i = 0; i < loopCount; i++) {
            bh.consume(loopCount);
        }
    }
}
