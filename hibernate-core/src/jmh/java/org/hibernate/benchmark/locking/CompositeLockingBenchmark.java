package org.hibernate.benchmark.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.Version;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
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

/// Uncontended, warm-plan query costs including session and transaction lifecycle.
/// SQL capture and correctness checks run only during trial setup.
///
/// @author Steve Ebersole
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
@Threads(1)
@State(Scope.Benchmark)
public class CompositeLockingBenchmark {
	@Param({"1", "10", "100", "1000"})
	public int rows;

	@Param({"FOLLOW_ON"})
	public String workload;

	private SessionFactory factory;
	private final Capture capture = new Capture();

	@Setup(Level.Trial)
	public void setup() {
		final var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.connection.driver_class", "org.h2.Driver" )
				.applySetting( "hibernate.connection.url", "jdbc:h2:mem:locking_benchmark" )
				.applySetting( "hibernate.hbm2ddl.auto", "create-drop" )
				.applySetting( "hibernate.cache.use_second_level_cache", false )
				.applySetting( "hibernate.cache.use_query_cache", false )
				.applySetting( "hibernate.show_sql", false )
				.applySetting( "hibernate.session_factory.statement_inspector", capture )
				.build();
		try {
			factory = new MetadataSources( registry ).addAnnotatedClass( Item.class )
					.buildMetadata().buildSessionFactory();
		}
		catch (RuntimeException | Error failure) {
			StandardServiceRegistryBuilder.destroy( registry );
			throw failure;
		}
		try (var session = factory.openSession()) {
			final var transaction = session.beginTransaction();
			for ( int id = 1; id <= 1000; id++ ) {
				final var item = new Item();
				item.id = new Identifier( 1, id );
				item.label = "item-" + id;
				item.quantity = id;
				session.persist( item );
			}
			transaction.commit();
		}
		for ( int repetition = 0; repetition < 2; repetition++ ) {
			capture.sql = new ArrayList<>();
			execute( true );
			final var sql = capture.sql;
			capture.sql = null;
			final long lockingStatements = sql.stream()
					.filter( statement -> statement.toLowerCase( Locale.ROOT ).contains( "for update" ) ).count();
			if ( "ORDINARY".equals( workload ) ? sql.size() != 1 || lockingStatements != 0
					: "DIRECT".equals( workload ) ? sql.size() != 1 || lockingStatements != 1
					: sql.size() < 2 || lockingStatements < 1
							|| sql.get( 0 ).toLowerCase( Locale.ROOT ).contains( "for update" ) ) {
				throw new IllegalStateException( "Unexpected SQL strategy: " + sql );
			}
			System.out.println( "LOCKING_VALIDATION workload=" + workload + " rows=" + rows
					+ " repetition=" + repetition + " statements=" + sql.size()
					+ " lockingStatements=" + lockingStatements + " sql=" + sql );
		}
	}

	@Benchmark
	public long query() {
		return execute( false );
	}

	private long execute(boolean validate) {
		try (var session = factory.openSession()) {
			final var transaction = session.beginTransaction();
			try {
				final var query = session.createQuery(
						"from CompositeLockingBenchmarkItem where id.number <= :rows order by id.number", Item.class )
						.setParameter( "rows", rows );
				if ( !"ORDINARY".equals( workload ) ) {
					query.setLockMode( jakarta.persistence.LockModeType.PESSIMISTIC_WRITE ).setFollowOnStrategy(
							"DIRECT".equals( workload ) ? Locking.FollowOn.DISALLOW : Locking.FollowOn.FORCE );
				}
				final var result = query.getResultList();
				long checksum = 0;
				for ( int index = 0; index < result.size(); index++ ) {
					final var item = result.get( index );
					checksum += item.id.number() + item.quantity + item.version + item.label.length();
					if ( validate && (item.id.number() != index + 1 || item.id.tenant() != 1 || item.quantity != item.id.number()
							|| item.version != 0 || !item.label.equals( "item-" + item.id.number() )
							|| !"ORDINARY".equals( workload )
							&& session.getCurrentLockMode( item ) != LockMode.PESSIMISTIC_WRITE) ) {
						throw new IllegalStateException( "Unexpected entity state or lock mode" );
					}
				}
				if ( validate && result.size() != rows ) {
					throw new IllegalStateException( "Unexpected result count: " + result.size() );
				}
				transaction.commit();
				return checksum;
			}
			catch (RuntimeException | Error failure) {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
				throw failure;
			}
		}
	}

	@TearDown(Level.Trial)
	public void close() {
		if ( factory != null ) {
			factory.close();
		}
	}

	/// Captures validation SQL; the timed path only checks a null reference.
	private static class Capture implements StatementInspector {
		private List<String> sql;

		@Override
		public String inspect(String statement) {
			if ( sql != null ) {
				sql.add( statement );
			}
			return statement;
		}
	}

	/// Fixed, versioned fixture without associations or generated identifiers.
	@Entity(name = "CompositeLockingBenchmarkItem")
	public static class Item {
		@jakarta.persistence.EmbeddedId
		private Identifier id;
		@Version
		private int version;
		private String label;
		private int quantity;
	}

	/// Two basic identifier components exercise acquisition identity reconstruction.
	@jakarta.persistence.Embeddable
	public record Identifier(int tenant, int number) {
	}

}
