/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
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
import org.hibernate.action.queue.internal.GraphBasedActionQueue;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.internal.AbstractFlushingEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEvent;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.AuxCounters;
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

/// Measures collection-flush processing at full-stack and pre-JDBC boundaries.
///
/// Database loading, collection initialization, and application mutation happen in
/// invocation setup and are excluded from the measured operation. [#collectionFlush]
/// measures an explicit `Session.flush()` or the query which makes an auto-flush
/// decision. [#collectionFlushPreparation] stops after queue-native preparation,
/// and [#collectionFlushPlanning] additionally performs decomposition and graph
/// planning while omitting physical execution. Invocation teardown rolls back the
/// transaction so every invocation observes the same persisted baseline.
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
	private static final MethodHandle CURRENT_THREAD_ALLOCATED_BYTES = currentThreadAllocatedBytesMethod();

	@Benchmark
	public long collectionFlush(FlushState state) {
		return state.executeMeasuredOperation();
	}

	/// Measures collection discovery and queue preparation without executing JDBC mutations.
	///
	/// This complements the full-stack benchmark by retaining real Hibernate metadata,
	/// persistent collection wrappers, lifecycle callbacks, mutation interpretation, and
	/// queue-native preparation while stopping before action decomposition and execution.
	@Benchmark
	public long collectionFlushPreparation(FlushState state) {
		return state.executePreparationOnly();
	}

	/// Measures the flush pipeline through construction of the graph-native flush plan,
	/// without executing the plan or issuing JDBC mutations.
	@Benchmark
	public long collectionFlushPlanning(PlanningState state) {
		return state.executePlanningOnly();
	}

	/// Reports allocations made strictly inside collection-flush planning.
	///
	/// Unlike the GC profiler's normalized allocation result, this counter excludes
	/// invocation setup and teardown. The primary score includes the small cost of
	/// reading the thread-allocation counter and is not a planning-time measurement.
	/// JMH aggregates `EVENTS` counter headlines by summing measurement iterations;
	/// inspect the raw per-iteration values, or divide the headline by the iteration
	/// count, to obtain the per-planning-operation byte count.
	@Benchmark
	public long collectionFlushPlanningAllocations(
			PlanningState state,
			PlanningAllocationCounters counters) {
		final long before = currentThreadAllocatedBytes();
		final long result = state.executePlanningAllocationProbe( counters );
		counters.planningBytes = currentThreadAllocatedBytes() - before;
		return result;
	}

	@AuxCounters(AuxCounters.Type.EVENTS)
	@State(Scope.Thread)
	public static class PlanningAllocationCounters {
		public long planningBytes;
		public long discoveryBytes;
		public long queuePreparationBytes;
		public long graphPlanningBytes;
		public long cleanupBytes;
	}

	private static MethodHandle currentThreadAllocatedBytesMethod() {
		try {
			final Class<?> threadMxBeanType = Class.forName( "com.sun.management.ThreadMXBean" );
			return MethodHandles.publicLookup()
					.findVirtual(
							threadMxBeanType,
							"getThreadAllocatedBytes",
							MethodType.methodType( long.class, long.class )
					)
					.bindTo( ManagementFactory.getThreadMXBean() );
		}
		catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError( e );
		}
	}

	private static long currentThreadAllocatedBytes() {
		try {
			return (long) CURRENT_THREAD_ALLOCATED_BYTES.invokeExact( Thread.currentThread().getId() );
		}
		catch (Throwable e) {
			throw new IllegalStateException( "Could not read current-thread allocation count", e );
		}
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

		private long executePreparationOnly() {
			return PreparationOnlyFlushListener.INSTANCE.prepare( (EventSource) session );
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
			verifySeededCollections();
		}

		private void verifySeededCollections() {
			try ( Session verificationSession = sessionFactory.openSession() ) {
				verifyRowCount( verificationSession, "setValues" );
				verifyRowCount( verificationSession, "bagValues" );
				verifyRowCount( verificationSession, "listValues" );
				verifyRowCount( verificationSession, "mapValues" );
			}
		}

		private static void verifyRowCount(Session session, String attributeName) {
			final long expected = 32L * 16L + 256L;
			final long actual = session.createQuery(
					"select count(value) from FlushOwner owner join owner." + attributeName + " value",
					Long.class
			).getSingleResult();
			if ( actual != expected ) {
				throw new IllegalStateException(
						"Expected " + expected + " seeded " + attributeName + " rows, but found " + actual
				);
			}
		}
	}

	@State(Scope.Thread)
	public static class PlanningState {
		@Param({ "CREATE_16_BY_16", "CREATE_1_BY_256" })
		public String scenario;

		private FlushState flushState;

		@Setup(Level.Trial)
		public void setUpTrial() {
			flushState = new FlushState();
			flushState.queueType = "graph";
			flushState.scenario = scenario;
			flushState.setUpTrial();
		}

		@TearDown(Level.Trial)
		public void tearDownTrial() {
			flushState.tearDownTrial();
		}

		@Setup(Level.Invocation)
		public void setUpInvocation() {
			flushState.setUpInvocation();
		}

		@TearDown(Level.Invocation)
		public void tearDownInvocation() {
			flushState.tearDownInvocation();
		}

		private long executePlanningOnly() {
			return PreparationOnlyFlushListener.INSTANCE.plan( (EventSource) flushState.session );
		}

		private long executePlanningAllocationProbe(PlanningAllocationCounters counters) {
			return PreparationOnlyFlushListener.INSTANCE.plan(
					(EventSource) flushState.session,
					counters
			);
		}
	}

	private static final class PreparationOnlyFlushListener extends AbstractFlushingEventListener {
		private static final PreparationOnlyFlushListener INSTANCE = new PreparationOnlyFlushListener();

		private long prepare(EventSource session) {
			final var persistenceContext = session.getPersistenceContextInternal();
			final FlushProcessingContext flushProcessingContext = prepareFlushProcessing( new FlushEvent( session ) );
			try {
				persistenceContext.setFlushing( true );
				session.getActionQueue().prepareActions();
				return session.getActionQueue().numberOfCollectionCreations()
						+ session.getActionQueue().numberOfCollectionUpdates()
						+ session.getActionQueue().numberOfCollectionRemovals();
			}
			finally {
				persistenceContext.setFlushing( false );
				clearFlushProcessing( persistenceContext );
			}
		}

		private long plan(EventSource session) {
			final var persistenceContext = session.getPersistenceContextInternal();
			final FlushProcessingContext flushProcessingContext = prepareFlushProcessing( new FlushEvent( session ) );
			try {
				persistenceContext.setFlushing( true );
				final var actionQueue = session.getActionQueue();
				actionQueue.prepareActions();
				if ( !(actionQueue instanceof GraphBasedActionQueue graphBasedActionQueue) ) {
					throw new IllegalStateException( "Flush planning benchmark requires the graph action queue" );
				}
				final var plan = graphBasedActionQueue.buildFlushPlan();
				if ( plan == null ) {
					return 0;
				}
				long result = plan.steps().size();
				for ( var step : plan.steps() ) {
					result += step.operations().size();
				}
				return result;
			}
			finally {
				persistenceContext.setFlushing( false );
				clearFlushProcessing( persistenceContext );
			}
		}

		private long plan(EventSource session, PlanningAllocationCounters counters) {
			final var persistenceContext = session.getPersistenceContextInternal();
			long before = currentThreadAllocatedBytes();
			final FlushProcessingContext flushProcessingContext = prepareFlushProcessing( new FlushEvent( session ) );
			counters.discoveryBytes = currentThreadAllocatedBytes() - before;
			try {
				persistenceContext.setFlushing( true );
				final var actionQueue = session.getActionQueue();
				before = currentThreadAllocatedBytes();
				actionQueue.prepareActions();
				counters.queuePreparationBytes = currentThreadAllocatedBytes() - before;
				if ( !(actionQueue instanceof GraphBasedActionQueue graphBasedActionQueue) ) {
					throw new IllegalStateException( "Flush planning benchmark requires the graph action queue" );
				}
				before = currentThreadAllocatedBytes();
				final var plan = graphBasedActionQueue.buildFlushPlan();
				counters.graphPlanningBytes = currentThreadAllocatedBytes() - before;
				if ( plan == null ) {
					return 0;
				}
				long result = plan.steps().size();
				for ( var step : plan.steps() ) {
					result += step.operations().size();
				}
				return result;
			}
			finally {
				before = currentThreadAllocatedBytes();
				persistenceContext.setFlushing( false );
				clearFlushProcessing( persistenceContext );
				counters.cleanupBytes = currentThreadAllocatedBytes() - before;
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
		CREATE_1_BY_256 {
			@Override
			void prepare(Session session) {
				session.persist( newOwner( -1L, 256 ) );
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
		},
		AUTOFLUSH_REQUIRED_CLEAN {
			@Override
			void prepare(Session session) {
				owner( session ).setValues.size();
			}

			@Override
			long execute(Session session) {
				return aggregateSetRowCount( session );
			}
		},
		AUTOFLUSH_REQUIRED_PREFLUSHED {
			@Override
			void prepare(Session session) {
				final var values = owner( session ).setValues;
				values.remove( value( 128 ) );
				values.add( "required-auto-flush-preflushed" );
				session.flush();
			}

			@Override
			long execute(Session session) {
				return aggregateSetRowCount( session );
			}
		},
		AUTOFLUSH_DECISION_CLEAN {
			@Override
			void prepare(Session session) {
				owner( session ).setValues.size();
			}

			@Override
			long execute(Session session) {
				return autoFlushSetTable( session );
			}
		},
		AUTOFLUSH_DECISION_REQUIRED {
			@Override
			void prepare(Session session) {
				final var values = owner( session ).setValues;
				values.remove( value( 128 ) );
				values.add( "required-auto-flush-decision" );
			}

			@Override
			long execute(Session session) {
				return autoFlushSetTable( session );
			}
		},
		AUTOFLUSH_DECISION_PREFLUSHED {
			@Override
			void prepare(Session session) {
				final var values = owner( session ).setValues;
				values.remove( value( 128 ) );
				values.add( "required-auto-flush-decision-preflushed" );
				session.flush();
			}

			@Override
			long execute(Session session) {
				return autoFlushSetTable( session );
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

		private static long aggregateSetRowCount(Session session) {
			return session.createQuery(
					"select count(value) from FlushOwner owner join owner.setValues value",
					Long.class
			).getSingleResult();
		}

		private static long autoFlushSetTable(Session session) {
			return ((SharedSessionContractImplementor) session)
					.autoFlushIfRequired( Set.of( "cfp_owner_sets" ) )
					? 1L
					: 0L;
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
