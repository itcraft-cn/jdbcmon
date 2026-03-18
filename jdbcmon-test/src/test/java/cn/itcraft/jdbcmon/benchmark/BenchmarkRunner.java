package cn.itcraft.jdbcmon.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        String include = args.length > 0 ? args[0] : "QueryBenchmark";
        
        Options opt = new OptionsBuilder()
                .include(".*" + include + ".*")
                .warmupIterations(5)
                .measurementIterations(3)
                .forks(1)
                .threads(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .jvmArgs("-XX:+UseG1GC")
                .build();

        new Runner(opt).run();
    }
}