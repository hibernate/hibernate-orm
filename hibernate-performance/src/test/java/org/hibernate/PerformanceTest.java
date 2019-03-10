package org.hibernate;

import java.util.concurrent.TimeUnit;

import org.hibernate.performance.BaseBenchmark;
import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.spf4j.stackmonitor.JmhFlightRecorderProfiler;
import org.spf4j.stackmonitor.Spf4jJmhProfiler;

public class PerformanceTest {

	
	/**
	 * Allow to run benchmark as Junit test
	 * 
	 * @throws RunnerException
	 */
	@Test 
	public void launchBenchmark() throws RunnerException{
		String destinationfolder=System.getProperty("basedir",org.spf4j.base.Runtime.USER_DIR)+"/target";
		Options opt = new OptionsBuilder()
                // Specify which benchmarks to run. 
                // You can be more specific if you'd like to run only one benchmark per test.
                .include(BaseBenchmark.class.getName() + ".*")
//                .include(".*")
// Set the following options as needed
                .addProfiler(JmhFlightRecorderProfiler.class)
                .addProfiler(Spf4jJmhProfiler.class)
                .jvmArgs("-Djmh.stack.profiles="+destinationfolder,
                		"-Djmh.fr.options=defaultrecording=true",
                		"-XX:+UnlockCommercialFeatures")
                .result(destinationfolder+"/benchmarksresults.json")
                .resultFormat(ResultFormatType.JSON)
	            .mode (Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(2)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(2)
                .threads(1)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                //.jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining")
                //.addProfiler(WinPerfAsmProfiler.class)
                .build();

        new Runner(opt).run();
	}
}