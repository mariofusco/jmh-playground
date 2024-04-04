package org.jmhplayground;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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
import org.openjdk.jmh.infra.Blackhole;

/**
 * Lessons learned:
 * 1. Bimorphic and polymorphic calls have a cost
 * 2. The cost can be negligible or not depending on the amount of other work performed
 *
 * see https://wiki.openjdk.org/display/HotSpot/MethodData
 *
 * use -prof "async:output=flamegraph;dir=/tmp;libPath=/home/mario/software/async-profiler-2.8.1-linux-x64/build/libasyncProfiler.so"
 * use -prof "async:output=flamegraph;dir=/tmp;libPath=/home/mario/software/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
public class JMH4_TypeProfilePollution {

    @Param({"0", "10"})
    private int work;

//    @Param({"0", "1", "2", "3"})
    @Param({"0", "3"})
    private int pollutionLevel;

    private record Person(String name, int age) { }

    private List<Person> persons;

    @Setup
    public void setup(Blackhole bh) {
        persons = List.of(new Person("John", 100));
        if (pollutionLevel == 0) {
            return;
        }
        for (int i = 0; i < 12_000; i++) {
            bh.consume(anyOf(persons, JMH4_TypeProfilePollution::isYoung));
            switch (pollutionLevel) {
                case 3:
                    bh.consume(anyOf(persons, JMH4_TypeProfilePollution::hasLongName));
                case 2:
                    bh.consume(anyOf(persons, JMH4_TypeProfilePollution::isSenior));
                case 1:
                    bh.consume(anyOf(persons, JMH4_TypeProfilePollution::hasShortName));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }


    @Benchmark
    public boolean anyYoung() {
        var work = this.work;
        if (work > 0) {
            Blackhole.consumeCPU(work);
        }
        return anyOf(persons, JMH4_TypeProfilePollution::isYoung);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static boolean anyOf(List<Person> persons, Predicate<Person> predicate) {
        for (Person person : persons) {
            if (predicate.test(person)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isYoung(Person person) {
        return person.age() < 18;
    }

    private static boolean isSenior(Person person) {
        return person.age() < 65;
    }

    private static boolean hasShortName(Person person) {
        return person.name().length() < 6;
    }

    private static boolean hasLongName(Person person) {
        return person.name().length() >= 6;
    }
}
