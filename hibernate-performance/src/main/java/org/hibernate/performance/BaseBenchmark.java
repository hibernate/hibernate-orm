package org.hibernate.performance;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.annotations.Scope;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 1)
@Measurement(iterations = 2)
@Warmup(iterations = 1)
@OutputTimeUnit(value = TimeUnit.MILLISECONDS)
public class BaseBenchmark extends BaseCoreFunctionalTestCase {
	
	@Benchmark
	@BenchmarkMode(value = { Mode.AverageTime })
	public void testInsertobject() {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and
		// provides the Session.
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Foo foo=new Foo();
		foo.setMessage("payload");
		s.save(foo);
		tx.commit();
		s.close();
	}
	
	@Benchmark
	@BenchmarkMode(value = { Mode.AverageTime })
	public void testInsertAndUpdateobject() {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and
		// provides the Session.
		Serializable id;
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Foo foo=new Foo();
		foo.setMessage("payload2");
		id=s.save(foo);
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		Foo foo2=s.get(Foo.class, id);
		foo2.setMessage("oh,no");
		s.save(foo);
		tx.commit();
		s.close();
	}

	/**
	 * ============================== HOW TO RUN THIS TEST: ====================================
	 * 
	 * You can see the benchmark runs as usual.
	 * 
	 * You can run this test:
	 *
	 * a) Via the command line: $ mvn clean install $ java -jar
	 * target/benchmarks.jar BaseBenchmark -wi 5 -i 5 -f 1 (we requested 5
	 * warmup/measurement iterations, single fork)
	 *
	 * 
	 * b) Via the Java API: (see the JMH homepage for possible caveats when running
	 * from IDE: http://openjdk.java.net/projects/code-tools/jmh/)
	 * 
	 */
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include(BaseBenchmark.class.getSimpleName()).build();
		new Runner(opt).run();

		/**
		 * use the following lines instead of jmh start to debug the code (run without
		 * jmh)
		 */
//		BaseBenchmark benchmark = new BaseBenchmark();
//		try {
//			benchmark.testInsertobject();
//		} catch (HibernateException e) {
//			e.printStackTrace();
//		} 
	}
	
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Foo.class, };
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[] {
//					"Foo.hbm.xml",
//					"Bar.hbm.xml"
		};
	}

	// If those mappings reside somewhere other than resources/org/hibernate/test,
	// change this.
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/performance/";
	}

	// Add in any settings that are specific to your test. See
	// resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);
//		configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
//		configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());
		configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}
	
	public BaseBenchmark() {
		buildSessionFactory();
		buildConfiguration();
	}
}