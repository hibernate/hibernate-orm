/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

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
import org.openjdk.jmh.annotations.Warmup;

/// Measures the full collection-flush processing path using revision-neutral ORM APIs.
///
/// Database loading, collection initialization, and application mutation happen in
/// invocation setup and are excluded from the measured operation. The benchmark
/// measures either an explicit `Session.flush()` or the query which makes an
/// auto-flush decision. Invocation teardown rolls back the transaction so every
/// invocation observes the same persisted baseline.
///
/// The benchmark is deliberately compatible with the collection-flush-processing
/// Stage 0 revision. Keep this source identical when comparing implementation
/// stages.
///
/// Approval runs should use at least three forks and should be repeated with the
/// JMH GC profiler. The annotation defaults are intended for development runs.
/// Because JMH's GC profiler observes invocation setup and teardown as well as
/// the timed method, allocation results describe the complete repeatable fixture
/// cycle; elapsed time describes only the measured flush or query operation.
///
/// @author Steve Ebersole
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
public class CollectionFlushProcessingBenchmark {
	@Benchmark
	public long collectionFlush(FlushState state) {
		return state.executeMeasuredOperation();
	}

	@State(Scope.Thread)
	public static class FlushState {
		@Param({ "legacy", "graph" })
		public String queueType;

		@Param({
				"CLEAN_32",
				"SET_SPARSE_256",
				"SET_CLEAR_256",
				"BAG_SPARSE_256",
				"LIST_SPARSE_256",
				"MAP_SPARSE_256",
				"CREATE_16_BY_16",
				"AUTOFLUSH_DISCARDED",
				"AUTOFLUSH_REQUIRED"
		})
		public String scenario;

		private SessionFactory sessionFactory;
		private Session session;
		private Transaction transaction;
		private Scenario selectedScenario;

		@Setup(Level.Trial)
		public void setUpTrial() {
			selectedScenario = Scenario.valueOf( scenario );
			sessionFactory = buildSessionFactory( queueType );
			seedDatabase();
		}

		@TearDown(Level.Trial)
		public void tearDownTrial() {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
		}

		@Setup(Level.Invocation)
		public void setUpInvocation() {
			session = sessionFactory.openSession();
			transaction = session.beginTransaction();
			selectedScenario.prepare( session );
		}

		@TearDown(Level.Invocation)
		public void tearDownInvocation() {
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}
			if ( session != null ) {
				session.close();
			}
		}

		private long executeMeasuredOperation() {
			return selectedScenario.execute( session );
		}

		private void seedDatabase() {
			try ( Session seedSession = sessionFactory.openSession() ) {
				final Transaction seedTransaction = seedSession.beginTransaction();
				for ( long id = 1; id <= 32; id++ ) {
					seedSession.persist( newOwner( id, 16 ) );
				}
				seedSession.persist( newOwner( 1000L, 256 ) );
				seedSession.persist( new BenchmarkMarker( 1L ) );
				seedTransaction.commit();
			}
		}
	}

	private enum Scenario {
		CLEAN_32 {
			@Override
			void prepare(Session session) {
				for ( long id = 1; id <= 32; id++ ) {
					session.find( FlushOwner.class, id ).setValues.size();
				}
			}
		},
		SET_SPARSE_256 {
			@Override
			void prepare(Session session) {
				final var values = owner( session ).setValues;
				values.remove( value( 128 ) );
				values.add( "set-replacement" );
			}
		},
		SET_CLEAR_256 {
			@Override
			void prepare(Session session) {
				owner( session ).setValues.clear();
			}
		},
		BAG_SPARSE_256 {
			@Override
			void prepare(Session session) {
				final var values = owner( session ).bagValues;
				values.remove( 128 );
				values.add( "bag-replacement" );
			}
		},
		LIST_SPARSE_256 {
			@Override
			void prepare(Session session) {
				owner( session ).listValues.set( 128, "list-replacement" );
			}
		},
		MAP_SPARSE_256 {
			@Override
			void prepare(Session session) {
				owner( session ).mapValues.put( key( 128 ), "map-replacement" );
			}
		},
		CREATE_16_BY_16 {
			@Override
			void prepare(Session session) {
				for ( long id = -1; id >= -16; id-- ) {
					session.persist( newOwner( id, 16 ) );
				}
			}
		},
		AUTOFLUSH_DISCARDED {
			@Override
			void prepare(Session session) {
				final var values = owner( session ).setValues;
				values.remove( value( 128 ) );
				values.add( "discarded-auto-flush" );
			}

			@Override
			long execute(Session session) {
				return session.createQuery( "from BenchmarkMarker", BenchmarkMarker.class )
						.getResultList()
						.size();
			}
		},
		AUTOFLUSH_REQUIRED {
			@Override
			void prepare(Session session) {
				final var values = owner( session ).setValues;
				values.remove( value( 128 ) );
				values.add( "required-auto-flush" );
			}

			@Override
			long execute(Session session) {
				return session.createQuery(
						"select count(value) from FlushOwner owner join owner.setValues value",
						Long.class
				).getSingleResult();
			}
		};

		abstract void prepare(Session session);

		long execute(Session session) {
			session.flush();
			return 0L;
		}

		FlushOwner owner(Session session) {
			return session.find( FlushOwner.class, 1000L );
		}
	}

	private static SessionFactory buildSessionFactory(String queueType) {
		final var registry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect" )
				.applySetting(
						AvailableSettings.URL,
						"jdbc:h2:mem:collection_flush_processing_" + queueType + ";DB_CLOSE_DELAY=-1"
				)
				.applySetting( AvailableSettings.USER, "sa" )
				.applySetting( AvailableSettings.PASS, "" )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.applySetting( AvailableSettings.SHOW_SQL, false )
				.applySetting( AvailableSettings.FORMAT_SQL, false )
				.applySetting( AvailableSettings.USE_SQL_COMMENTS, false )
				.applySetting( AvailableSettings.STATEMENT_BATCH_SIZE, 50 )
				.applySetting( AvailableSettings.FLUSH_QUEUE_TYPE, queueType )
				.build();

		return new MetadataSources( registry )
				.addAnnotatedClass( FlushOwner.class )
				.addAnnotatedClass( BenchmarkMarker.class )
				.buildMetadata()
				.buildSessionFactory();
	}

	private static FlushOwner newOwner(long id, int size) {
		final var owner = new FlushOwner( id );
		for ( int i = 0; i < size; i++ ) {
			final String value = value( i );
			owner.setValues.add( value );
			owner.bagValues.add( value );
			owner.listValues.add( value );
			owner.mapValues.put( key( i ), value );
		}
		return owner;
	}

	private static String key(int index) {
		return "k" + index;
	}

	private static String value(int index) {
		return "v" + index;
	}

	@Entity(name = "FlushOwner")
	@Table(name = "cfp_owner")
	public static class FlushOwner {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(name = "cfp_owner_sets")
		@Column(name = "set_value")
		private Set<String> setValues = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "cfp_owner_bags")
		@Column(name = "bag_value")
		private List<String> bagValues = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "cfp_owner_lists")
		@OrderColumn(name = "list_position")
		@Column(name = "list_value")
		private List<String> listValues = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "cfp_owner_maps")
		@MapKeyColumn(name = "map_key")
		@Column(name = "map_value")
		private Map<String, String> mapValues = new HashMap<>();

		public FlushOwner() {
		}

		private FlushOwner(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "BenchmarkMarker")
	@Table(name = "cfp_marker")
	public static class BenchmarkMarker {
		@Id
		private Long id;

		public BenchmarkMarker() {
		}

		private BenchmarkMarker(Long id) {
			this.id = id;
		}
	}
}
