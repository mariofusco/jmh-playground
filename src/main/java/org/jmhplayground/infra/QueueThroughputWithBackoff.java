package org.jmhplayground.infra;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * This benchmark measures the throughput of different queues implementation.
 * Capturing the behaviour of a dynamic data structure like a queue isn't easy and requires to both add
 * telemetry (in the form of additional counters) and control mechanisms.
 * The goal is to manipulate the dynamic of the experiment, constraining its state
 * to the one we are interested in and detecting when we failed to do so.
 *
 * Run me with org.jmhplayground.infra.QueueThroughputWithBackoff.naive -pqCapacity=-1 -pdelayProducer=0 -pdelayConsumer=0, -pbackoff=NONE
 *
 * Run me with org.jmhplayground.infra.QueueThroughputWithBackoff.telemetry -pqCapacity=-1 -pdelayProducer=0 -pdelayConsumer=0, -pbackoff=NONE -prof gc -rf json
 * Run me with org.jmhplayground.infra.QueueThroughputWithBackoff.telemetry -pqCapacity=-1 -pdelayProducer=0 -pdelayConsumer=100, -pbackoff=NONE -prof gc -rf json
 * Run me with org.jmhplayground.infra.QueueThroughputWithBackoff.telemetry -pqCapacity=32768 -pdelayProducer=0 -pdelayConsumer=0, -pbackoff=NONE -prof gc -rf json
 */
@State(Scope.Group)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
public class QueueThroughputWithBackoff {

    static final Integer TEST_ELEMENT = 1;
    Integer element;
    Queue<Integer> q;

    @Param
    QueueFactories qType;

    @Param
    BackoffPolicy backoff;

    @Param(value = { "-1", "128", "32768" })
    int qCapacity;

    @Param(value = { "0", "100" })
    int delayProducer;

    @Param(value = { "0", "100" })
    int delayConsumer;

    public enum QueueFactories implements IntFunction<Queue> {
        CONCURRENT_LINKED_QUEUE,
        LINKED_BLOCKING_QUEUE,
        ARRAY_BLOCKING_QUEUE;

        @Override
        public Queue apply(int value) {
            return switch (this) {
                case CONCURRENT_LINKED_QUEUE -> new ConcurrentLinkedQueue();
                case LINKED_BLOCKING_QUEUE -> value < 0 ? new LinkedBlockingQueue() : new LinkedBlockingQueue(value);
                case ARRAY_BLOCKING_QUEUE -> value < 0 ? null : new ArrayBlockingQueue(value);
            };
        }
    }

    public enum BackoffPolicy implements Runnable {
        NONE {
            @Override
            public void run() { }
        },
        YIELD {
            @Override
            public void run() {
                Thread.yield();
            }
        },
        PAUSE {
            @Override
            public void run() {
                Thread.onSpinWait();
            }
        },
        SLEEP {
            @Override
            public void run() {
                LockSupport.parkNanos(1L);
            }
        };
    }

    @Setup()
    public void createQandPrimeCompilation() {
        this.element = TEST_ELEMENT;
        this.q = qType.apply(qCapacity);
        if (q == null) {
            System.exit(0);
        }
    }

    @AuxCounters
    @State(Scope.Thread)
    public static class PollCounters {
        public long emptyQueue;
        public long pollsMade;
    }

    @AuxCounters
    @State(Scope.Thread)
    public static class OfferCounters {
        public long fullQueue;
        public long offersMade;
    }

    private void backoff() {
        backoff.run();
    }

    @Benchmark
    @Group("telemetry")
    @GroupThreads(1)
    public boolean telemetryOffer(OfferCounters counters) {
        boolean fullQueue = !q.offer(element);
        if (fullQueue) {
            counters.fullQueue++;
            backoff();
        } else {
            counters.offersMade++;
        }
        var delay = delayProducer;
        if (delay != 0) {
            Blackhole.consumeCPU(delay);
        }
        return fullQueue;
    }

    @Benchmark
    @Group("telemetry")
    @GroupThreads(1)
    public boolean telemetryPoll(PollCounters counters) {
        Integer e = q.poll();
        boolean emptyQueue = e == null;
        if (e == null) {
            counters.emptyQueue++;
            backoff();
        } else {
            counters.pollsMade++;
        }
        var delay = delayConsumer;
        if (delay != 0) {
            Blackhole.consumeCPU(delay);
        }
        return emptyQueue;
    }

    @Benchmark
    @Group("naive")
    @GroupThreads(1)
    public boolean naiveOffer() {
        return q.offer(element);
    }

    @Benchmark
    @Group("naive")
    @GroupThreads(1)
    public Object naivePoll() {
        return q.poll();
    }

    @TearDown(Level.Iteration)
    public void emptyQ() {
        q.clear();
    }
}
