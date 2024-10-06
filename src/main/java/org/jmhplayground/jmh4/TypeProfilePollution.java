package org.jmhplayground.jmh4;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Lessons learned:
 * 1. Mono and Bimorphic are relatively cheap but megamorphic calls have a cost
 * 2. The cost can be negligible or not depending on the amount of other work performed
 * 3. Wrong JIT warmup decisions can affect performance on steady state
 *
 * Run with
 * -prof "async:output=flamegraph;dir=/tmp;libPath=/home/mario/software/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so;rawCommand=features=vtable"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
public class TypeProfilePollution {

    public enum Morphism {
        MONO, BI, MEGA
    }

    /**
     * This is just to make more evident any performance difference between the different morphisms
     */
    @Param({"64"})
    public int count;

    @Param({"0", "20"})
    private int job;

    @Param
    private Morphism morphism;

    @Param({"false", "true"})
    private boolean polluteAtWarmup;

    private record Person(String name, int age) { }

    private Person person;

    private Predicate<Person>[] predicates;

    private static final Predicate<Person> PREDICATE_0 = TypeProfilePollution::predicate0;
    private static final Predicate<Person> PREDICATE_1 = TypeProfilePollution::predicate1;
    private static final Predicate<Person> PREDICATE_2 = TypeProfilePollution::predicate2;
    private static final Predicate<Person> PREDICATE_3 = TypeProfilePollution::predicate3;

    static {
        // validate that the predicates have all different classes
        var classes = Stream.of(PREDICATE_0, PREDICATE_1, PREDICATE_2, PREDICATE_3)
                .map(Object::getClass)
                .collect(Collectors.toSet());
        if (classes.size() != 4) {
            throw new IllegalStateException("Predicates must have different classes");
        }
    }

    @Setup
    public void setup() {
        person = new Person("John", 100);
        // I know i know this is cheating - but it's to make sure the iterations of the benchmark are the same
        // regardless the morphism
        predicates = new Predicate[4 * count];
        // always use the same number of predicates to avoid introducing differences in the benchmark
        switch (morphism) {
            case MONO:
                Arrays.fill(predicates, PREDICATE_0);
                break;
            case BI:
                for (int i = 0; i < predicates.length; i += 2) {
                    predicates[i] = PREDICATE_0;
                    predicates[i + 1] = PREDICATE_1;
                }
                break;
            case MEGA:
                for (int i = 0; i < predicates.length; i += 4) {
                    predicates[i] = PREDICATE_0;
                    predicates[i + 1] = PREDICATE_1;
                    predicates[i + 2] = PREDICATE_2;
                    predicates[i + 3] = PREDICATE_3;
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }

        if (polluteAtWarmup) {
            // pollute the type profile using the configured morphism
            for (int i = 0; i < 15_000; i++) {
                testPredicates();
            }
            // force it back to be monomorphic
            Arrays.fill(predicates, PREDICATE_0);
        }
    }

    @Benchmark
    public void testPredicates() {
        var job = this.job;
        var person = this.person;
        for (Predicate<Person> predicate : predicates) {
            if (job > 0) {
                Blackhole.consumeCPU(job);
            }
            // no need to use a black hole here, as testWith is forcibly not inlined!
            testWith(person, predicate);
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static boolean testWith(Person person, Predicate<Person> predicate) {
        return predicate.test(person);
    }

    /**
     * 4 different and always false cheap predicates
     */
    private static boolean predicate0(Person person) {
        return person.age() == 0x65;
    }

    private static boolean predicate1(Person person) {
        return person.age() == 0x66;
    }

    private static boolean predicate2(Person person) {
        return person.age() == 0x67;
    }

    private static boolean predicate3(Person person) {
        return person.age() == 0x68;
    }
}
