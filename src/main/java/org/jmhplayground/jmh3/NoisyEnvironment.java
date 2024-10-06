package org.jmhplayground.jmh3;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * This benchmark shows the effect of running a benchmark in a noisy environment.
 *
 * In order to properly run experiments it's important to reduce at minimum the effects of the OS scheduler decisions
 * and governor. Such machine should have fixed or known frequencies under known conditions and the experiments should
 * try to not overcommit over its existing resources, unless it is the purpose of the experiment itself.
 *
 * Run with
 * -prof perf
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
public class NoisyEnvironment {

    @Param({"10"})
    private int work;

    @Param({"0", "4", "8"})
    private int noisyNeighbors;

    private List<Process> noisyProcesses;

    @Setup
    public void setup() throws InterruptedException {
        noisyProcesses = IntStream.range(0, noisyNeighbors).mapToObj(i -> startNoisyNeighbor()).toList();
    }

    // spawn a process executing sha1sum /dev/zero to simulate a noisy neighbor:
    // it could be achieved as well using a bash script performing a tight loop
    private static Process startNoisyNeighbor() {
        try {
            return new ProcessBuilder("sha1sum", "/dev/zero").start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TearDown
    public void tearDown() {
        noisyProcesses.forEach(Process::destroy);
    }

    @Benchmark
    public void consumeCpu() {
        Blackhole.consumeCPU(work);
    }
}