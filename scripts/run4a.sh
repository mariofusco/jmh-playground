java -jar target/benchmark.jar -ppolluteAtWarmup=false -prof "async:output=flamegraph;dir=/tmp;libPath=$HOME/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so;rawCommand=features=vtable" org.jmhplayground.jmh4.TypeProfilePollution.*