package org.jmhplayground.jmh4;

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
 * see https://blogs.oracle.com/javamagazine/post/mastering-the-mechanics-of-java-method-invocation
 *
 * Run with
 * -prof "async:output=flamegraph;dir=/tmp;libPath=/home/mario/software/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so;rawCommand=features=vtable"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
public class InvokeInterface {

    //    @Param({"0", "1", "2", "3"})
    @Param({"0", "3"})
    private int pollutionLevel;

    @Param({"0", "10"})
    private int job;

    private record Person(String name, int age) { }

    private List<Person> persons;

    private Predicate<Person>[] predicates;

    @Setup
    public void setup() {
        persons = List.of(new Person("John", 100));

        predicates = new Predicate[pollutionLevel+1];
        switch (pollutionLevel) {
            case 3:
                predicates[3] = InvokeInterface::hasLongName;
            case 2:
                predicates[2] = InvokeInterface::isSenior;
            case 1:
                predicates[1] = InvokeInterface::hasShortName;
            case 0:
                predicates[0] = InvokeInterface::isYoung;
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }


    @Benchmark
    public void testPredicate(Blackhole bh) {
        var work = this.job;
        int pos = 0;
        for (int i = 0; i < 12; i++) {
            if (work > 0) {
                Blackhole.consumeCPU(work);
            }
            bh.consume(anyOf(persons, predicates[pos]));
            pos = pos == pollutionLevel ? 0 : pos+1;
        }
    }

    /**
     * Just-in-time remembers the receiver for virtual calls: sometimes the jit can further refine the arity
     * of a virtual call making
     */
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
        return person.age() >= 65;
    }

    private static boolean hasShortName(Person person) {
        return person.name().length() < 6;
    }

    private static boolean hasLongName(Person person) {
        return person.name().length() >= 6;
    }
}
