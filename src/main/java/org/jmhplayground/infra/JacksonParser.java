package org.jmhplayground.infra;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonRecyclerPools;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 20, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx15M", "-XX:SoftRefLRUPolicyMSPerMB=1"})
public class JacksonParser {

    private byte[] oldGenBuffer;

    private JsonFactory jsonFactory = JsonFactory.builder()
            .recyclerPool(JsonRecyclerPools.threadLocalPool())
            .build();
    private String json = "{'a':123,'b':'foobar'}".replace('\'', '"');

    @Setup
    public void setup() {
        oldGenBuffer = new byte[8 * 1024 * 1024];
    }

    @Benchmark
    @Threads(8)
    public void parseJson() {
        try (JsonParser parser = jsonFactory.createParser("{'a':123,'b':'foobar'}".replace('\'', '"'))) {
            assertSame(JsonToken.START_OBJECT, parser.nextToken());
            assertSame(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("a", parser.currentName());
            assertSame(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(123, parser.getIntValue());
            assertSame(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("b", parser.currentName());
            assertSame(JsonToken.VALUE_STRING, parser.nextToken());
            assertEquals("foobar", parser.getText());
            assertSame(JsonToken.END_OBJECT, parser.nextToken());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException();
        }
    }

    private static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new IllegalStateException();
        }
    }
}
