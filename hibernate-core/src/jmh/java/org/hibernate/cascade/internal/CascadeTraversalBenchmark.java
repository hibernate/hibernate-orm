/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.cascade.spi.CascadePropertySelection;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadingActions;
import org.hibernate.cascade.spi.PropertySelectionKind;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.EvictEvent;
import org.hibernate.event.spi.EvictEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.RefreshContext;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.AuxCounters;
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

/// Measures metadata-driven cascade traversal using a fully bootstrapped
/// Hibernate mapping model, real persisters, and real session state.
///
/// The entity graphs and session are prepared outside the measured operation.
/// Terminal ORM event listeners and transient checks are replaced with an
/// equivalent counting sink. This makes traversal observable
/// to JMH while excluding database access and downstream entity-event work from
/// the comparison.
///
/// `productionMetadataTraversal` measures the ordinary cascade entry and is the
/// stable production boundary. `directMetadataWalker` invokes the same walker
/// and context creation while excluding production action validation.
///
/// `metadataStructuralMetrics` publishes deterministic counts collected by one
/// decision-only traversal during
/// trial setup. Their reported time and allocation are not performance results;
/// use the clean traversal methods with `-prof gc` for those measurements.
///
/// Approval runs use `-f 3 -wi 10 -i 20 -prof gc`; the smaller annotation
/// defaults are intended for development runs.
///
/// @author Steve Ebersole
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
public class CascadeTraversalBenchmark {
	@Benchmark
	public long productionMetadataTraversal(TraversalState state) {
		return state.traverseProduction();
	}

	@Benchmark
	public long directMetadataWalker(TraversalState state) {
		return state.traverseDirect( state.propertySelection );
	}

	@Benchmark
	public long ordinaryMetadataWalker(TraversalState state) {
		return state.traverseDirect( state.ordinaryPropertySelection );
	}

	@Benchmark
	public PropertySelectionKind propertySelectionResolution(TraversalState state) {
		return state.resolvePropertySelection();
	}

	@Benchmark
	public void metadataStructuralMetrics(TraversalState state, StructuralCounters counters) {
		state.publishStructuralMetrics( counters );
	}

	@AuxCounters(AuxCounters.Type.EVENTS)
	@State(Scope.Thread)
	public static class StructuralCounters {
		public long metadataPropertiesScanned;
		public long propertyValueResolutions;
		public long attributePathAllocations;
		public long selectedProperties;
		public long selectionNone;
		public long selectionAll;
		public long selectionSelected;
	}

	@State(Scope.Thread)
	public static class TraversalState {
		@Param({
				"NO_WORK",
				"SPARSE_4",
				"SPARSE_8",
				"SPARSE_16",
				"SPARSE_32",
				"SPARSE_64",
				"SPARSE_256",
				"DENSE_TO_ONE",
				"COMPONENT_DEPTH_1",
				"COMPONENT_DEPTH_4",
				"COMPONENT_DEPTH_8",
				"COLLECTION_EMPTY",
				"COLLECTION_10",
				"COLLECTION_100",
				"ENHANCED_LOADED",
				"ENHANCED_UNLOADED"
		})
		public String shape;

		@Param({
				"PERSIST_BEFORE_INSERT",
				"PERSIST_AFTER_INSERT",
				"PERSIST_ON_FLUSH",
				"REMOVE_BEFORE_DELETE",
				"REMOVE_AFTER_DELETE",
				"MERGE",
				"REFRESH",
				"EVICT",
				"CHECK_ON_FLUSH"
		})
		public String route;

		private SessionFactory sessionFactory;
		private EventSource session;
		private EntityPersister persister;
		private Object root;
		private CascadingAction<Object> action;
		private CascadePoint cascadePoint;
		private TerminalEffectSink terminalEffectSink;
		private StructuralMetrics structuralMetrics;
		private CascadePropertySelection propertySelection;
		private CascadePropertySelection ordinaryPropertySelection;

		@Setup(Level.Trial)
		public void setUp() {
			final var fixtureShape = FixtureShape.valueOf( shape );
			final var routeKind = Route.valueOf( route );
			terminalEffectSink = new TerminalEffectSink();
			sessionFactory = buildSessionFactory( fixtureShape, routeKind, terminalEffectSink );
			final var factory = sessionFactory.unwrap( SessionFactoryImplementor.class );
			final var listenerRegistry = factory.getEventEngine().getListenerRegistry();
			listenerRegistry.setListeners( EventType.PERSIST, terminalEffectSink );
			listenerRegistry.setListeners( EventType.PERSIST_ONFLUSH, terminalEffectSink );
			listenerRegistry.setListeners( EventType.DELETE, terminalEffectSink );
			listenerRegistry.setListeners( EventType.MERGE, terminalEffectSink );
			listenerRegistry.setListeners( EventType.REFRESH, terminalEffectSink );
			listenerRegistry.setListeners( EventType.EVICT, terminalEffectSink );
			session = (EventSource) sessionFactory.openSession();
			persister = fixtureShape.enhancedState == EnhancedState.NONE
					? factory.getMappingMetamodel().getEntityDescriptor( fixtureShape.entityName() )
					: findEnhancedPersister( factory, CascadeTraversalBenchmarkModel.Root.class.getName() );
			root = fixtureShape.enhancedState == EnhancedState.NONE
					? fixtureShape.createRoot()
					: createEnhancedRoot( factory, fixtureShape, session );
			action = routeKind.action();
			cascadePoint = routeKind.cascadePoint();
			propertySelection = persister instanceof AbstractEntityPersister abstractPersister
					? abstractPersister.getCascadePropertySelection( action )
					: CascadePropertySelection.all();
			ordinaryPropertySelection = CascadePropertySelection.all();
			structuralMetrics = collectStructuralMetrics();
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			if ( session != null ) {
				session.close();
			}
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
		}

		private long traverseDirect(CascadePropertySelection selection) {
			final long before = terminalEffectSink.invocationCount();
			CascadeWalker.instance().traverse(
					new CascadeTraversalContext<>(
							action,
							cascadePoint,
							session,
							persister,
							root,
							null
					),
					selection
			);
			return terminalEffectSink.invocationCount() - before;
		}

		private PropertySelectionKind resolvePropertySelection() {
			return persister instanceof AbstractEntityPersister abstractPersister
					? abstractPersister.getCascadePropertySelection( action ).getKind()
					: PropertySelectionKind.ALL;
		}

		private long traverseProduction() {
			final long before = terminalEffectSink.invocationCount();
			Cascade.cascade(
					action,
					cascadePoint,
					session,
					persister,
					root,
					null
			);
			return terminalEffectSink.invocationCount() - before;
		}

		private StructuralMetrics collectStructuralMetrics() {
			final var collector = new StructuralTraceCollector();
			Cascade.cascade(
					action,
					cascadePoint,
					session,
					persister,
					root,
					null,
					collector,
					CascadeEffectMode.DECISION_ONLY
			);
			return collector.metrics();
		}

		private void publishStructuralMetrics(StructuralCounters counters) {
			counters.metadataPropertiesScanned = structuralMetrics.nodesConsidered();
			counters.propertyValueResolutions = structuralMetrics.propertyValueResolutions();
			counters.attributePathAllocations = structuralMetrics.attributePathAllocations();
			counters.selectedProperties = switch ( propertySelection.getKind() ) {
				case NONE -> 0;
				case ALL -> persister.getPropertyTypes().length;
				case SELECTED -> propertySelection.getSelectedPropertyCount();
			};
			counters.selectionNone = propertySelection.getKind() == PropertySelectionKind.NONE
					? 1
					: 0;
			counters.selectionAll = propertySelection.getKind() == PropertySelectionKind.ALL
					? 1
					: 0;
			counters.selectionSelected =
					propertySelection.getKind() == PropertySelectionKind.SELECTED ? 1 : 0;
		}
	}

	private record StructuralMetrics(
			long nodesConsidered,
			long propertyValueResolutions,
			long attributePathAllocations) {
	}

	private static class StructuralTraceCollector implements CascadeDecisionTrace {
		private long nodesConsidered;
		private long propertyValueResolutions;
		private long attributePathAllocations;

		@Override
		public void record(CascadeTraceEvent event) {
			if ( event instanceof CascadeTraceEvent.Node ) {
				nodesConsidered++;
			}
			else if ( event instanceof CascadeTraceEvent.Value ) {
				propertyValueResolutions++;
			}
		}

		@Override
		public void pathAllocated() {
			attributePathAllocations++;
		}

		private StructuralMetrics metrics() {
			return new StructuralMetrics(
					nodesConsidered,
					propertyValueResolutions,
					attributePathAllocations
			);
		}
	}

	private enum Route {
		PERSIST_BEFORE_INSERT( CascadingActions.PERSIST, CascadePoint.BEFORE_INSERT_AFTER_DELETE ),
		PERSIST_AFTER_INSERT( CascadingActions.PERSIST, CascadePoint.AFTER_INSERT_BEFORE_DELETE ),
		PERSIST_ON_FLUSH( CascadingActions.PERSIST_ON_FLUSH, CascadePoint.BEFORE_FLUSH ),
		REMOVE_BEFORE_DELETE( CascadingActions.REMOVE, CascadePoint.AFTER_INSERT_BEFORE_DELETE ),
		REMOVE_AFTER_DELETE( CascadingActions.REMOVE, CascadePoint.BEFORE_INSERT_AFTER_DELETE ),
		MERGE( CascadingActions.MERGE, CascadePoint.BEFORE_MERGE ),
		REFRESH( CascadingActions.REFRESH, CascadePoint.BEFORE_REFRESH ),
		EVICT( CascadingActions.EVICT, CascadePoint.AFTER_EVICT ),
		CHECK_ON_FLUSH( CascadingActions.CHECK_ON_FLUSH, CascadePoint.BEFORE_FLUSH );

		private final CascadingAction<?> action;
		private final CascadePoint cascadePoint;

		Route(CascadingAction<?> action, CascadePoint cascadePoint) {
			this.action = action;
			this.cascadePoint = cascadePoint;
		}

		@SuppressWarnings("unchecked")
		private CascadingAction<Object> action() {
			return (CascadingAction<Object>) action;
		}

		private CascadePoint cascadePoint() {
			return cascadePoint;
		}
	}

	private enum FixtureShape {
		NO_WORK( "CascadeNoWork", 0, 0, 0, 0 ),
		SPARSE_4( "CascadeSparse4", 4, 2, 0, 0 ),
		SPARSE_8( "CascadeSparse8", 8, 2, 0, 0 ),
		SPARSE_16( "CascadeSparse16", 16, 2, 0, 0 ),
		SPARSE_32( "CascadeSparse32", 32, 2, 0, 0 ),
		SPARSE_64( "CascadeSparse64", 64, 2, 0, 0 ),
		SPARSE_256( "CascadeSparse256", 256, 2, 0, 0 ),
		DENSE_TO_ONE( "CascadeDenseToOne", 64, 56, 0, 0 ),
		COMPONENT_DEPTH_1( "CascadeComponentDepth1", 0, 0, 1, 0 ),
		COMPONENT_DEPTH_4( "CascadeComponentDepth4", 0, 0, 4, 0 ),
		COMPONENT_DEPTH_8( "CascadeComponentDepth8", 0, 0, 8, 0 ),
		COLLECTION_EMPTY( "CascadeCollectionEmpty", 0, 0, 0, 0, true ),
		COLLECTION_10( "CascadeCollection10", 0, 0, 0, 10, true ),
		COLLECTION_100( "CascadeCollection100", 0, 0, 0, 100, true ),
		ENHANCED_LOADED( "EnhancedCascadeRoot", EnhancedState.LOADED ),
		ENHANCED_UNLOADED( "EnhancedCascadeRoot", EnhancedState.UNLOADED );

		private static final int COLLECTION_COUNT = 4;

		private final String entityName;
		private final int propertyCount;
		private final int associationCount;
		private final int componentDepth;
		private final int collectionSize;
		private final boolean collections;
		private final EnhancedState enhancedState;

		FixtureShape(
				String entityName,
				int propertyCount,
				int associationCount,
				int componentDepth,
				int collectionSize) {
			this( entityName, propertyCount, associationCount, componentDepth, collectionSize, false );
		}

		FixtureShape(
				String entityName,
				int propertyCount,
				int associationCount,
				int componentDepth,
				int collectionSize,
				boolean collections) {
			this.entityName = entityName;
			this.propertyCount = propertyCount;
			this.associationCount = associationCount;
			this.componentDepth = componentDepth;
			this.collectionSize = collectionSize;
			this.collections = collections;
			this.enhancedState = EnhancedState.NONE;
		}

		FixtureShape(String entityName, EnhancedState enhancedState) {
			this.entityName = entityName;
			this.propertyCount = 0;
			this.associationCount = 0;
			this.componentDepth = 0;
			this.collectionSize = 0;
			this.collections = false;
			this.enhancedState = enhancedState;
		}

		private String entityName() {
			return entityName;
		}

		private Map<String, Object> createRoot() {
			final Map<String, Object> root = new HashMap<>();
			root.put( "id", 1L );
			for ( int i = 0; i < propertyCount - associationCount; i++ ) {
				root.put( "basic" + i, "value" );
			}
			for ( int i = 0; i < associationCount; i++ ) {
				root.put( "association" + i, child( i ) );
			}
			if ( componentDepth > 0 ) {
				root.put( "component1", component( 1 ) );
			}
			if ( collections ) {
				for ( int collectionIndex = 0; collectionIndex < COLLECTION_COUNT; collectionIndex++ ) {
					final List<Map<String, Object>> children = new ArrayList<>( collectionSize );
					for ( int elementIndex = 0; elementIndex < collectionSize; elementIndex++ ) {
						children.add( child( collectionIndex * collectionSize + elementIndex ) );
					}
					root.put( "collection" + collectionIndex, children );
				}
			}
			return root;
		}

		private Map<String, Object> component(int level) {
			final Map<String, Object> component = new HashMap<>();
			if ( level == componentDepth ) {
				component.put( "association", child( level ) );
			}
			else {
				component.put( "component" + ( level + 1 ), component( level + 1 ) );
			}
			return component;
		}

		private static Map<String, Object> child(int id) {
			final Map<String, Object> child = new HashMap<>();
			child.put( "id", (long) id + 1000 );
			child.put( "name", "child" );
			return child;
		}
	}

	private enum EnhancedState {
		NONE,
		LOADED,
		UNLOADED
	}

	private static SessionFactory buildSessionFactory(
			FixtureShape shape,
			Route route,
			TerminalEffectSink terminalEffectSink) {
		if ( shape.enhancedState != EnhancedState.NONE ) {
			return buildEnhancedSessionFactory( terminalEffectSink );
		}
		final var registry = serviceRegistryBuilder( terminalEffectSink )
				.build();
		final var mapping = mapping( shape, route );
		return new MetadataSources( registry )
				.addInputStream( new ByteArrayInputStream( mapping.getBytes( StandardCharsets.UTF_8 ) ) )
				.buildMetadata()
				.buildSessionFactory();
	}

	private static StandardServiceRegistryBuilder serviceRegistryBuilder(Interceptor interceptor) {
		return new StandardServiceRegistryBuilder()
				.clearSettings()
				.applySetting( AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect" )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "none" )
				.applySetting( AvailableSettings.SHOW_SQL, false )
				.applySetting( AvailableSettings.INTERCEPTOR, interceptor );
	}

	private static SessionFactory buildEnhancedSessionFactory(TerminalEffectSink terminalEffectSink) {
		final var registry = serviceRegistryBuilder( terminalEffectSink )
				.build();
		return new MetadataSources( registry )
					.addAnnotatedClass( CascadeTraversalBenchmarkModel.Root.class )
					.addAnnotatedClass( CascadeTraversalBenchmarkModel.Child.class )
					.buildMetadata()
					.buildSessionFactory();
	}

	private static Object createEnhancedRoot(
			SessionFactoryImplementor factory,
			FixtureShape shape,
			EventSource session) {
		final var rootPersister = findEnhancedPersister(
				factory,
				CascadeTraversalBenchmarkModel.Root.class.getName()
		);
		final var childPersister = findEnhancedPersister(
				factory,
				CascadeTraversalBenchmarkModel.Child.class.getName()
		);
		final var root = rootPersister.instantiate( 1L, session );
		final var child = childPersister.instantiate( 2L, session );
		rootPersister.setValue( root, rootPersister.getPropertyIndex( "child" ), child );
		final var interceptor = rootPersister.getBytecodeEnhancementMetadata()
				.injectInterceptor( root, 1L, session );
		if ( shape.enhancedState == EnhancedState.LOADED ) {
			interceptor.attributeInitialized( "child" );
		}
		session.getPersistenceContextInternal().addEntry(
				root,
				Status.MANAGED,
				new Object[] { child },
				null,
				1L,
				null,
				LockMode.NONE,
				true,
				rootPersister
		);
		return root;
	}

	private static EntityPersister findEnhancedPersister(
			SessionFactoryImplementor factory,
			String mappedClassName) {
		final EntityPersister[] match = new EntityPersister[1];
		factory.getMappingMetamodel().forEachEntityDescriptor( candidate -> {
			if ( candidate.getMappedClass().getName().equals( mappedClassName ) ) {
				match[0] = candidate;
			}
		} );
		if ( match[0] == null ) {
			throw new IllegalStateException( "No persister for enhanced benchmark class " + mappedClassName );
		}
		return match[0];
	}

	private static String mapping(FixtureShape shape, Route route) {
		final String cascade = route == Route.CHECK_ON_FLUSH ? "" : " cascade=\"all\"";
		final var mapping = new StringBuilder( 32_768 );
		mapping.append( "<?xml version=\"1.0\"?>\n" )
				.append( "<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" " )
				.append( "\"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">\n" )
				.append( "<hibernate-mapping default-lazy=\"false\">\n" )
				.append( "  <class entity-name=\"CascadeChild\" table=\"cascade_child\">\n" )
				.append( "    <id name=\"id\" type=\"long\" column=\"id\"/>\n" )
				.append( "    <property name=\"name\" type=\"string\" column=\"name\"/>\n" )
				.append( "  </class>\n" )
				.append( "  <class entity-name=\"" ).append( shape.entityName )
				.append( "\" table=\"cascade_root\">\n" )
				.append( "    <id name=\"id\" type=\"long\" column=\"id\"/>\n" );

		for ( int i = 0; i < shape.propertyCount - shape.associationCount; i++ ) {
			mapping.append( "    <property name=\"basic" ).append( i )
					.append( "\" type=\"string\" column=\"basic_" ).append( i ).append( "\"/>\n" );
		}
		for ( int i = 0; i < shape.associationCount; i++ ) {
			mapping.append( "    <many-to-one name=\"association" ).append( i )
					.append( "\" entity-name=\"CascadeChild\"" ).append( cascade ).append( " column=\"child_" )
					.append( i ).append( "\"/>\n" );
		}
		if ( shape.componentDepth > 0 ) {
			appendComponent( mapping, 1, shape.componentDepth, cascade );
		}
		if ( shape.collections ) {
			for ( int i = 0; i < FixtureShape.COLLECTION_COUNT; i++ ) {
				mapping.append( "    <bag name=\"collection" ).append( i )
						.append( "\"" ).append( cascade ).append( " table=\"cascade_collection_" ).append( i )
						.append( "\">\n" )
						.append( "      <key column=\"root_id\"/>\n" )
						.append( "      <one-to-many entity-name=\"CascadeChild\"/>\n" )
						.append( "    </bag>\n" );
			}
		}

		return mapping.append( "  </class>\n</hibernate-mapping>\n" ).toString();
	}

	private static void appendComponent(StringBuilder mapping, int level, int depth, String cascade) {
		mapping.append( "    ".repeat( level ) )
				.append( "<dynamic-component name=\"component" ).append( level ).append( "\">\n" );
		if ( level == depth ) {
			mapping.append( "    ".repeat( level + 1 ) )
					.append( "<many-to-one name=\"association\" entity-name=\"CascadeChild\" " )
					.append( cascade ).append( " column=\"component_child\"/>\n" );
		}
		else {
			appendComponent( mapping, level + 1, depth, cascade );
		}
		mapping.append( "    ".repeat( level ) ).append( "</dynamic-component>\n" );
	}

	private static class TerminalEffectSink
			implements Interceptor, PersistEventListener, DeleteEventListener, MergeEventListener,
			RefreshEventListener, EvictEventListener {
		private long invocationCount;

		private long invocationCount() {
			return invocationCount;
		}

		@Override
		public Boolean isTransient(Object entity) {
			invocationCount++;
			return false;
		}

		@Override
		public void onPersist(PersistEvent event) {
			invocationCount++;
		}

		@Override
		public void onPersist(PersistEvent event, PersistContext createdAlready) {
			invocationCount++;
		}

		@Override
		public void onDelete(DeleteEvent event) {
			invocationCount++;
		}

		@Override
		public void onDelete(DeleteEvent event, DeleteContext transientEntities) {
			invocationCount++;
		}

		@Override
		public void onMerge(MergeEvent event) {
			invocationCount++;
		}

		@Override
		public void onMerge(MergeEvent event, MergeContext copiedAlready) {
			invocationCount++;
		}

		@Override
		public void onRefresh(RefreshEvent event) {
			invocationCount++;
		}

		@Override
		public void onRefresh(RefreshEvent event, RefreshContext refreshedAlready) {
			invocationCount++;
		}

		@Override
		public void onEvict(EvictEvent event) {
			invocationCount++;
		}
	}
}
