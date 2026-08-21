/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadeStyles;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadingActions;
import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Normalized-trace behavior tests for the metadata cascade walker.
///
/// @author Steve Ebersole
class CascadeBehaviorTest {
	@Test
	void rootFastPathBehavior() {
		traceBehavior( CascadeBehaviorTest::rootFastPathFixture );
	}

	@Test
	void rootToOneBehavior() {
		traceBehavior( CascadeBehaviorTest::rootToOneFixture );
	}

	@Test
	void nestedComponentBehavior() {
		traceBehavior( CascadeBehaviorTest::nestedComponentFixture );
	}

	@Test
	void collectionElementBehavior() {
		final var trace = traceBehavior( CascadeBehaviorTest::collectionElementFixture );

		assertThat( event( trace, CascadeTraceEvent.CollectionIterator.class ).mode() )
				.isEqualTo( CascadeTraceEvent.CollectionIteratorMode.CUSTOM );
		assertThat( trace.stream()
				.filter( CascadeTraceEvent.Action.class::isInstance )
				.map( CascadeTraceEvent.Action.class::cast )
				.map( CascadeTraceEvent.Action::location )
				.map( CascadeTraceEvent.Location::cascadePoint )
				.toList() ).containsExactly(
				CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION,
				CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION
		);
	}

	@ParameterizedTest(name = "{0} at {1} with {2}")
	@MethodSource("builtInActionRoutes")
	void builtInActionRouteBehavior(
			CascadingAction<?> action,
			CascadePoint cascadePoint,
			CascadeStyle style,
			ForeignKeyDirection direction) {
		final var trace = traceBehavior( () -> builtInToOneFixture(
				action,
				cascadePoint,
				style,
				direction,
				OnDeleteAction.NO_ACTION
		) );

		final var node = event( trace, CascadeTraceEvent.Node.class );
		final boolean applies = action != CascadingActions.REFRESH;
		assertThat( node.applies() ).isEqualTo( applies );
		if ( applies ) {
			assertThat( event( trace, CascadeTraceEvent.Association.class ).traversed() ).isTrue();
			assertThat( event( trace, CascadeTraceEvent.Action.class ).decision() )
					.isEqualTo( action == CascadingActions.PERSIST_ON_FLUSH
							? CascadeTraceEvent.ActionDecision.SKIPPED_BY_STYLE
							: CascadeTraceEvent.ActionDecision.SUPPRESSED_BY_DECISION_ONLY );
		}
	}

	@ParameterizedTest(name = "MERGE at {0}")
	@EnumSource(CascadePoint.class)
	void cascadePointBehavior(CascadePoint cascadePoint) {
		final var direction = cascadePoint == CascadePoint.AFTER_INSERT_BEFORE_DELETE
				? ForeignKeyDirection.TO_PARENT
				: ForeignKeyDirection.FROM_PARENT;
		final var trace = traceBehavior( () -> builtInToOneFixture(
				CascadingActions.MERGE,
				cascadePoint,
				CascadeStyles.MERGE,
				direction,
				OnDeleteAction.NO_ACTION
		) );
		assertThat( event( trace, CascadeTraceEvent.Association.class ).traversed() ).isTrue();
	}

	@ParameterizedTest(name = "{0} at {1}")
	@MethodSource("associationDirections")
	void associationDirectionBehavior(ForeignKeyDirection direction, CascadePoint cascadePoint) {
		final var trace = traceBehavior( () -> builtInToOneFixture(
				CascadingActions.MERGE,
				cascadePoint,
				CascadeStyles.MERGE,
				direction,
				OnDeleteAction.NO_ACTION
		) );
		assertThat( event( trace, CascadeTraceEvent.Association.class ).traversed() )
				.isEqualTo( direction.cascadeNow( cascadePoint ) );
	}

	@ParameterizedTest(name = "CHECK_ON_FLUSH with OnDelete {0}")
	@EnumSource(OnDeleteAction.class)
	void databaseCascadeBehavior(OnDeleteAction onDeleteAction) {
		final var trace = traceBehavior( () -> builtInToOneFixture(
				CascadingActions.CHECK_ON_FLUSH,
				CascadePoint.BEFORE_FLUSH,
				CascadeStyles.NONE,
				ForeignKeyDirection.TO_PARENT,
				onDeleteAction
		) );
		assertThat( event( trace, CascadeTraceEvent.Node.class ).databaseCascade() )
				.isEqualTo( onDeleteAction == OnDeleteAction.CASCADE );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("enhancedToOneScenarios")
	void enhancedToOneLazyStateBehavior(EnhancedToOneScenario scenario) {
		final var trace = traceBehavior( () -> enhancedToOneFixture( scenario ) );
		final var rootDecisions = trace.stream()
				.filter( CascadeTraceEvent.Root.class::isInstance )
				.map( CascadeTraceEvent.Root.class::cast )
				.map( CascadeTraceEvent.Root::decision )
				.toList();

		if ( scenario.managedWithoutLoadedState() ) {
			assertThat( rootDecisions ).containsExactly(
					CascadeTraceEvent.RootDecision.TRAVERSE,
					CascadeTraceEvent.RootDecision.MANAGED_WITHOUT_LOADED_STATE
			);
			assertThat( trace ).noneMatch( CascadeTraceEvent.Node.class::isInstance );
			return;
		}

		assertThat( rootDecisions ).containsExactly( CascadeTraceEvent.RootDecision.TRAVERSE );
		assertThat( event( trace, CascadeTraceEvent.Lazy.class ).decision() )
				.isEqualTo( scenario.lazyDecision() );
		final var valueResolutions = trace.stream()
				.filter( CascadeTraceEvent.Value.class::isInstance )
				.map( CascadeTraceEvent.Value.class::cast )
				.map( CascadeTraceEvent.Value::resolution )
				.toList();
		if ( scenario.valueResolution() == null ) {
			assertThat( valueResolutions ).isEmpty();
		}
		else {
			assertThat( valueResolutions ).containsExactly( scenario.valueResolution() );
		}
	}

	@Test
	void managedUninitializedCollectionAcquiresWrapperBehavior() {
		final var trace = traceBehavior( CascadeBehaviorTest::managedLazyCollectionFixture );

		assertThat( event( trace, CascadeTraceEvent.Lazy.class ).decision() )
				.isEqualTo( CascadeTraceEvent.LazyDecision.COLLECTION_WRAPPER );
		assertThat( event( trace, CascadeTraceEvent.Value.class ).resolution() )
				.isEqualTo( CascadeTraceEvent.ValueResolution.LAZY_COLLECTION_WRAPPER );
		assertThat( event( trace, CascadeTraceEvent.Association.class ).traversed() ).isFalse();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("collectionIterationScenarios")
	void collectionIteratorStateBehavior(CollectionIterationScenario scenario) {
		final var trace = traceBehavior( () -> builtInCollectionFixture( scenario ) );

		assertThat( event( trace, CascadeTraceEvent.CollectionIterator.class ).mode() )
				.isEqualTo( scenario.iteratorMode() );
		assertThat( trace.stream()
				.filter( CascadeTraceEvent.Node.class::isInstance )
				.map( CascadeTraceEvent.Node.class::cast )
				.filter( node -> node.location().nodeKind() == CascadeTraceEvent.NodeKind.TO_ONE ) )
				.hasSize( scenario.expectedElements() );
		assertThat( event( trace, CascadeTraceEvent.Orphan.class ).source() )
				.isEqualTo( scenario.initialized()
						? CascadeTraceEvent.OrphanSource.COLLECTION_LOADED_STATE
						: CascadeTraceEvent.OrphanSource.QUEUED_OPERATIONS );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("orphanCollectionScenarios")
	void collectionHolderAndNewWrapperOrphanBehavior(OrphanCollectionScenario scenario) {
		final var trace = traceBehavior( () -> orphanCollectionFixture( scenario ) );
		final var orphan = event( trace, CascadeTraceEvent.Orphan.class );

		assertThat( orphan.eligibility() ).isEqualTo( scenario.eligibility() );
		assertThat( orphan.source() ).isEqualTo( scenario.source() );
		assertThat( orphan.timing() ).isEqualTo( scenario.eligibility() == CascadeTraceEvent.OrphanEligibility.ELIGIBLE
				? CascadeTraceEvent.OrphanTiming.DURING_COLLECTION_TRAVERSAL
				: CascadeTraceEvent.OrphanTiming.NONE );
	}

	@Test
	void componentCollectionElementBehavior() {
		final var trace = traceBehavior( CascadeBehaviorTest::componentCollectionFixture );

		assertThat( trace.stream()
				.filter( CascadeTraceEvent.Action.class::isInstance )
				.map( CascadeTraceEvent.Action.class::cast )
				.map( CascadeTraceEvent.Action::location )
				.map( CascadeTraceEvent.Location::path )
				.toList() ).containsExactly(
				List.of( "components", "association" ),
				List.of( "components", "association" )
		);
	}

	@Test
	void deepMixedComponentBehavior() {
		final var trace = traceBehavior( CascadeBehaviorTest::deepMixedComponentFixture );

		assertThat( trace.stream()
				.filter( CascadeTraceEvent.Node.class::isInstance )
				.map( CascadeTraceEvent.Node.class::cast )
				.map( node -> node.location().nodeKind() + ":" + String.join( ".", node.location().path() ) )
				.toList() ).containsExactly(
				"BASIC:rootBasic",
				"COMPONENT:outer",
				"BASIC:outer.outerBasic",
				"COMPONENT:outer.inner",
				"BASIC:outer.inner.innerBasic",
				"TO_ONE:outer.inner.entity",
				"ANY:outer.inner.any",
				"COLLECTION:outer.inner.children",
				"TO_ONE:outer.inner.children",
				"COMPONENT:outer.missing",
				"ANY:rootAny"
		);
		assertThat( trace.stream()
				.filter( CascadeTraceEvent.Action.class::isInstance )
				.map( CascadeTraceEvent.Action.class::cast )
				.map( action -> String.join( ".", action.location().path() ) )
				.toList() ).containsExactly(
				"outer.inner.entity",
				"outer.inner.any",
				"outer.inner.children",
				"rootAny"
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("logicalOneToOneScenarios")
	void logicalOneToOneReplacementTimingBehavior(LogicalOneToOneScenario scenario) {
		final var trace = traceBehavior( () -> logicalOneToOneFixture( scenario ) );
		final var orphanEvents = trace.stream()
				.filter( CascadeTraceEvent.Orphan.class::isInstance )
				.map( CascadeTraceEvent.Orphan.class::cast )
				.toList();

		assertThat( orphanEvents ).extracting( CascadeTraceEvent.Orphan::eligibility )
				.containsExactly(
						CascadeTraceEvent.OrphanEligibility.ELIGIBLE,
						CascadeTraceEvent.OrphanEligibility.ORPHANED
				);
		assertThat( orphanEvents.get( 1 ).source() )
				.isEqualTo( CascadeTraceEvent.OrphanSource.ENTITY_LOADED_STATE );
		assertThat( orphanEvents.get( 1 ).timing() ).isEqualTo( scenario.timing() );
		assertThat( trace ).anyMatch( event -> event instanceof CascadeTraceEvent.Value value
				&& value.resolution() == CascadeTraceEvent.ValueResolution.LOADED_STATE );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("collectionDatabaseCascadeScenarios")
	void collectionDatabaseCascadeDirectionBehavior(CollectionDatabaseCascadeScenario scenario) {
		final var trace = traceBehavior( () -> collectionDatabaseCascadeFixture( scenario ) );
		final var elementNodes = trace.stream()
				.filter( CascadeTraceEvent.Node.class::isInstance )
				.map( CascadeTraceEvent.Node.class::cast )
				.filter( node -> node.location().nodeKind() == CascadeTraceEvent.NodeKind.TO_ONE )
				.toList();

		assertThat( elementNodes ).singleElement()
				.extracting( CascadeTraceEvent.Node::databaseCascade )
				.isEqualTo( scenario.expectedDatabaseCascade() );
	}

	private static List<CascadeTraceEvent> traceBehavior(Supplier<Fixture> fixtureFactory) {
		final var fixture = fixtureFactory.get();
		final var trace = fixture.trace();
		assertThat( trace ).isNotEmpty();
		return trace;
	}

	private static <E extends CascadeTraceEvent> E event(
			List<CascadeTraceEvent> trace,
			Class<E> eventType) {
		return trace.stream()
				.filter( eventType::isInstance )
				.map( eventType::cast )
				.findFirst()
				.orElseThrow();
	}

	private static Stream<Arguments> builtInActionRoutes() {
		return Stream.of(
				Arguments.of(
						CascadingActions.REMOVE,
						CascadePoint.AFTER_INSERT_BEFORE_DELETE,
						CascadeStyles.DELETE,
						ForeignKeyDirection.TO_PARENT
				),
				Arguments.of(
						CascadingActions.REMOVE,
						CascadePoint.BEFORE_INSERT_AFTER_DELETE,
						CascadeStyles.ALL_DELETE_ORPHAN,
						ForeignKeyDirection.FROM_PARENT
				),
				Arguments.of(
						CascadingActions.REFRESH,
						CascadePoint.BEFORE_REFRESH,
						CascadeStyles.REFRESH,
						ForeignKeyDirection.FROM_PARENT
				),
				Arguments.of(
						CascadingActions.EVICT,
						CascadePoint.AFTER_EVICT,
						CascadeStyles.EVICT,
						ForeignKeyDirection.FROM_PARENT
				),
				Arguments.of(
						CascadingActions.MERGE,
						CascadePoint.BEFORE_MERGE,
						CascadeStyles.MERGE,
						ForeignKeyDirection.FROM_PARENT
				),
				Arguments.of(
						CascadingActions.PERSIST,
						CascadePoint.AFTER_INSERT_BEFORE_DELETE,
						CascadeStyles.PERSIST,
						ForeignKeyDirection.TO_PARENT
				),
				Arguments.of(
						CascadingActions.PERSIST,
						CascadePoint.BEFORE_INSERT_AFTER_DELETE,
						CascadeStyles.ALL,
						ForeignKeyDirection.FROM_PARENT
				),
				Arguments.of(
						CascadingActions.PERSIST_ON_FLUSH,
						CascadePoint.BEFORE_FLUSH,
						CascadeStyles.DELETE_ORPHAN,
						ForeignKeyDirection.FROM_PARENT
				),
				Arguments.of(
						CascadingActions.CHECK_ON_FLUSH,
						CascadePoint.BEFORE_FLUSH,
						CascadeStyles.NONE,
						ForeignKeyDirection.FROM_PARENT
				)
		);
	}

	private static Stream<Arguments> associationDirections() {
		return Stream.of(
				Arguments.of( ForeignKeyDirection.FROM_PARENT, CascadePoint.AFTER_INSERT_BEFORE_DELETE ),
				Arguments.of( ForeignKeyDirection.TO_PARENT, CascadePoint.AFTER_INSERT_BEFORE_DELETE ),
				Arguments.of( ForeignKeyDirection.FROM_PARENT, CascadePoint.BEFORE_INSERT_AFTER_DELETE ),
				Arguments.of( ForeignKeyDirection.TO_PARENT, CascadePoint.BEFORE_INSERT_AFTER_DELETE )
		);
	}

	private static Stream<EnhancedToOneScenario> enhancedToOneScenarios() {
		return Stream.of(
				new EnhancedToOneScenario(
						"managed loaded attribute",
						true,
						true,
						false,
						false,
						CascadeTraceEvent.LazyDecision.LOADED,
						CascadeTraceEvent.ValueResolution.ENTITY_PROPERTY
				),
				new EnhancedToOneScenario(
						"detached loaded attribute",
						false,
						true,
						false,
						false,
						CascadeTraceEvent.LazyDecision.LOADED,
						CascadeTraceEvent.ValueResolution.ENTITY_PROPERTY
				),
				new EnhancedToOneScenario(
						"managed unloaded attribute fetched for action",
						true,
						false,
						true,
						false,
						CascadeTraceEvent.LazyDecision.TO_ONE_FETCH,
						CascadeTraceEvent.ValueResolution.LAZY_TO_ONE_FETCH
				),
				new EnhancedToOneScenario(
						"managed unloaded attribute skipped for action",
						true,
						false,
						false,
						false,
						CascadeTraceEvent.LazyDecision.ACTION_SKIP,
						null
				),
				new EnhancedToOneScenario(
						"detached unloaded attribute skipped without fetch",
						false,
						false,
						true,
						false,
						CascadeTraceEvent.LazyDecision.DETACHED_PARENT_SKIP,
						null
				),
				new EnhancedToOneScenario(
						"managed root without loaded state",
						true,
						false,
						true,
						true,
						null,
						null
				)
		);
	}

	private static Stream<CollectionIterationScenario> collectionIterationScenarios() {
		return Stream.of(
				new CollectionIterationScenario(
						"remove uses all elements for an uninitialized collection",
						CascadingActions.REMOVE,
						CascadeStyles.ALL,
						false,
						false,
						CascadeTraceEvent.CollectionIteratorMode.ALL,
						2
				),
				new CollectionIterationScenario(
						"persist uses queued additions for an uninitialized collection",
						CascadingActions.PERSIST,
						CascadeStyles.PERSIST,
						false,
						false,
						CascadeTraceEvent.CollectionIteratorMode.LOADED,
						2
				),
				new CollectionIterationScenario(
						"persist uses current elements for an initialized collection",
						CascadingActions.PERSIST,
						CascadeStyles.PERSIST,
						true,
						false,
						CascadeTraceEvent.CollectionIteratorMode.LOADED,
						2
				),
				new CollectionIterationScenario(
						"check-on-flush skips inverse collection elements",
						CascadingActions.CHECK_ON_FLUSH,
						CascadeStyles.NONE,
						false,
						true,
						CascadeTraceEvent.CollectionIteratorMode.EMPTY,
						0
				),
				new CollectionIterationScenario(
						"check-on-flush visits queued additions for an owned collection",
						CascadingActions.CHECK_ON_FLUSH,
						CascadeStyles.NONE,
						false,
						false,
						CascadeTraceEvent.CollectionIteratorMode.LOADED,
						2
				)
		);
	}

	private static Stream<OrphanCollectionScenario> orphanCollectionScenarios() {
		return Stream.of(
				new OrphanCollectionScenario(
						"holder-backed uninitialized collection uses queued orphans",
						true,
						false,
						false,
						CascadeTraceEvent.OrphanSource.QUEUED_OPERATIONS,
						CascadeTraceEvent.OrphanEligibility.ELIGIBLE
				),
				new OrphanCollectionScenario(
						"newly instantiated wrapper cannot have orphans",
						false,
						true,
						false,
						CascadeTraceEvent.OrphanSource.QUEUED_OPERATIONS,
						CascadeTraceEvent.OrphanEligibility.INELIGIBLE
				),
				new OrphanCollectionScenario(
						"initialized wrapper uses collection loaded-state orphans",
						false,
						false,
						true,
						CascadeTraceEvent.OrphanSource.COLLECTION_LOADED_STATE,
						CascadeTraceEvent.OrphanEligibility.ELIGIBLE
				)
		);
	}

	private static Stream<LogicalOneToOneScenario> logicalOneToOneScenarios() {
		return Stream.of(
				new LogicalOneToOneScenario(
						"to-parent one-to-one removes the orphan before updates",
						ForeignKeyDirection.TO_PARENT,
						CascadeTraceEvent.OrphanTiming.BEFORE_UPDATES
				),
				new LogicalOneToOneScenario(
						"from-parent one-to-one removes the orphan after updates",
						ForeignKeyDirection.FROM_PARENT,
						CascadeTraceEvent.OrphanTiming.AFTER_UPDATES
				)
		);
	}

	private static Stream<CollectionDatabaseCascadeScenario> collectionDatabaseCascadeScenarios() {
		return Stream.of(
				new CollectionDatabaseCascadeScenario(
						"from-parent action observes collection database cascade",
						ForeignKeyDirection.FROM_PARENT,
						true,
						true
				),
				new CollectionDatabaseCascadeScenario(
						"to-parent action does not observe collection database cascade",
						ForeignKeyDirection.TO_PARENT,
						true,
						false
				),
				new CollectionDatabaseCascadeScenario(
						"disabled collection database cascade remains disabled",
						ForeignKeyDirection.FROM_PARENT,
						false,
						false
				)
		);
	}

	private static Fixture enhancedToOneFixture(EnhancedToOneScenario scenario) {
		final var action = action();
		final var style = style( "STYLE_ENHANCED_TO_ONE" );
		final var entityType = mock( EntityType.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var interceptor = mock( LazyAttributeLoadingInterceptor.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var entry = scenario.entryPresent() ? mock( EntityEntry.class ) : null;
		final var root = new Object();
		final var child = new Object();

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persistenceContext.getEntry( root ) ).thenReturn( entry );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new EntityType[] { entityType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "child" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( child );
		when( enhancementMetadata.isEnhancedForLazyLoading() ).thenReturn( true );
		when( enhancementMetadata.hasUnFetchedAttributes( root ) ).thenReturn( !scenario.attributeLoaded() );
		when( enhancementMetadata.isAttributeLoaded( root, "child" ) ).thenReturn( scenario.attributeLoaded() );
		when( enhancementMetadata.extractInterceptor( root ) ).thenReturn( interceptor );
		when( interceptor.fetchAttribute( root, "child" ) ).thenReturn( child );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( entityType, style ) ).thenReturn( true );
		when( action.performOnLazyProperty() ).thenReturn( scenario.performOnLazyProperty() );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, entityType, factory ) ).thenReturn( false );
		if ( entry != null ) {
			when( entry.getLoadedState() )
					.thenReturn( scenario.managedWithoutLoadedState() ? null : new Object[] { child } );
			when( entry.getStatus() ).thenReturn( Status.MANAGED );
		}

		return new Fixture(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				root,
				false,
				() -> verifyEnhancedToOneAccess( scenario, persister, enhancementMetadata, interceptor, root )
		);
	}

	private static void verifyEnhancedToOneAccess(
			EnhancedToOneScenario scenario,
			EntityPersister persister,
			BytecodeEnhancementMetadata enhancementMetadata,
			LazyAttributeLoadingInterceptor interceptor,
			Object root) {
		if ( scenario.managedWithoutLoadedState() ) {
			verify( enhancementMetadata, never() ).hasUnFetchedAttributes( root );
			verify( persister, never() ).getValue( root, 0 );
			verify( interceptor, never() ).fetchAttribute( root, "child" );
		}
		else if ( scenario.lazyDecision() == CascadeTraceEvent.LazyDecision.LOADED ) {
			verify( persister ).getValue( root, 0 );
			verify( interceptor, never() ).fetchAttribute( root, "child" );
		}
		else if ( scenario.lazyDecision() == CascadeTraceEvent.LazyDecision.TO_ONE_FETCH ) {
			verify( persister, never() ).getValue( root, 0 );
			verify( interceptor ).fetchAttribute( root, "child" );
		}
		else {
			verify( persister, never() ).getValue( root, 0 );
			verify( interceptor, never() ).fetchAttribute( root, "child" );
		}
	}

	private static Fixture managedLazyCollectionFixture() {
		final var action = action();
		final var style = style( "STYLE_LAZY_COLLECTION" );
		final var collectionType = mock( CollectionType.class );
		final var elementType = mock( EntityType.class );
		final var collectionPersister = mock( CollectionPersister.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var entry = mock( EntityEntry.class );
		final var wrapper = mock( PersistentCollection.class );
		final var root = new Object();
		final String role = "Root.children";

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persistenceContext.getEntry( root ) ).thenReturn( entry );
		when( entry.getLoadedState() ).thenReturn( new Object[] { wrapper } );
		when( entry.getStatus() ).thenReturn( Status.MANAGED );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new CollectionType[] { collectionType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "children" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( enhancementMetadata.isEnhancedForLazyLoading() ).thenReturn( true );
		when( enhancementMetadata.hasUnFetchedAttributes( root ) ).thenReturn( true );
		when( enhancementMetadata.isAttributeLoaded( root, "children" ) ).thenReturn( false );
		when( collectionType.getRole() ).thenReturn( role );
		when( collectionType.getKeyOfOwner( root, session ) ).thenReturn( 1L );
		when( collectionType.getCollection( 1L, session, root, null ) ).thenReturn( wrapper );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( collectionType, style ) ).thenReturn( true );
		when( action.performOnLazyProperty() ).thenReturn( false );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, collectionType, factory ) ).thenReturn( false );

		return new Fixture(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				root,
				false,
				() -> {
					verify( collectionType ).getCollection( 1L, session, root, null );
					verify( persister, never() ).getValue( root, 0 );
				}
		);
	}

	@SuppressWarnings("removal")
	private static Fixture builtInCollectionFixture(CollectionIterationScenario scenario) {
		final var collectionType = mock( CollectionType.class );
		final var elementType = mock( EntityType.class );
		final var collectionPersister = mock( CollectionPersister.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var factoryOptions = mock( SessionFactoryOptions.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var collection = persistentCollection();
		final var root = new Object();
		final var first = new Object();
		final var second = new Object();
		final var consumed = new ArrayList<Object>();
		final String role = "Root.children";

		when( session.getFactory() ).thenReturn( factory );
		when( session.getSessionFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( factory.getSessionFactoryOptions() ).thenReturn( factoryOptions );
		when( factoryOptions.isUnownedAssociationTransientCheck() )
				.thenReturn( scenario.inverse() );
		when( persister.getFactory() ).thenReturn( factory );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new CollectionType[] { collectionType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "children" } );
		when( persister.getPropertyCascadeStyles() )
				.thenReturn( new CascadeStyle[] { scenario.style() } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( collection );
		when( persister.hasCascades() ).thenReturn( true );
		when( persister.hasCascadeDelete() ).thenReturn( true );
		when( persister.hasCascadePersist() ).thenReturn( true );
		when( persister.hasOwnedCollections() ).thenReturn( !scenario.inverse() );
		when( persister.hasCollections() ).thenReturn( true );
		when( collectionType.getRole() ).thenReturn( role );
		when( collectionType.isAssociationType() ).thenReturn( true );
		when( collectionType.getForeignKeyDirection() ).thenReturn( ForeignKeyDirection.FROM_PARENT );
		when( collectionType.isInverse( factory ) ).thenReturn( scenario.inverse() );
		when( collectionType.getElementType( factory ) ).thenReturn( elementType );
		when( elementType.getForeignKeyDirection() ).thenReturn( ForeignKeyDirection.FROM_PARENT );
		when( collection.wasInitialized() ).thenReturn( scenario.initialized() );
		when( collectionType.getElementsIterator( collection ) )
				.thenAnswer( invocation -> recordingIterator( List.of( first, second ), consumed ) );
		when( collection.queuedAdditionIterator() )
				.thenAnswer( invocation -> recordingIterator( List.of( first, second ), consumed ) );

		final var mappingMetamodel = mappingMetamodel( factory );
		when( mappingMetamodel.getCollectionDescriptor( role ) ).thenReturn( collectionPersister );
		when( collectionPersister.getElementType() ).thenReturn( elementType );
		when( collectionPersister.isCascadeDeleteEnabled() ).thenReturn( false );

		return new Fixture(
				castAction( scenario.action() ),
				CascadePoint.BEFORE_FLUSH,
				session,
				persister,
				root,
				true,
				() -> verifyCollectionIteration( scenario, collectionType, collection, consumed, first, second )
		);
	}

	private static void verifyCollectionIteration(
			CollectionIterationScenario scenario,
			CollectionType collectionType,
			PersistentCollection<?> collection,
			List<Object> consumed,
			Object first,
			Object second) {
		if ( scenario.iteratorMode() == CascadeTraceEvent.CollectionIteratorMode.EMPTY ) {
			verify( collectionType, never() ).getElementsIterator( collection );
			verify( collection, never() ).queuedAdditionIterator();
			assertThat( consumed ).isEmpty();
		}
		else if ( scenario.iteratorMode() == CascadeTraceEvent.CollectionIteratorMode.ALL
				|| scenario.initialized() ) {
			verify( collectionType ).getElementsIterator( collection );
			verify( collection, never() ).queuedAdditionIterator();
			assertThat( consumed ).containsExactly( first, second );
		}
		else {
			verify( collectionType, never() ).getElementsIterator( collection );
			verify( collection ).queuedAdditionIterator();
			assertThat( consumed ).containsExactly( first, second );
		}
	}

	@SuppressWarnings("removal")
	private static Fixture orphanCollectionFixture(OrphanCollectionScenario scenario) {
		final var action = action();
		final var style = style( "STYLE_ORPHAN_COLLECTION" );
		final var collectionType = mock( CollectionType.class );
		final var elementType = mock( EntityType.class );
		final var collectionPersister = mock( CollectionPersister.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var collectionEntry = mock( CollectionEntry.class );
		final var holder = persistentCollection();
		final Object collection = scenario.holderBacked() ? new Object() : holder;
		final var root = new Object();
		final var orphan = new Object();
		final String role = "Root.children";

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new CollectionType[] { collectionType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "children" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( collection );
		when( collectionType.getRole() ).thenReturn( role );
		when( collectionType.getAssociatedEntityName( factory ) ).thenReturn( "Element" );
		when( elementType.getAssociatedEntityName() ).thenReturn( "Element" );
		doReturn( scenario.holderBacked() ? holder : null )
				.when( persistenceContext ).getCollectionHolder( collection );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( collectionType, style ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_FLUSH, collectionType, factory ) ).thenReturn( true );
		when( action.directionAffectedByCascadeDelete() ).thenReturn( ForeignKeyDirection.FROM_PARENT );
		when( action.getCascadableChildrenIterator( session, collectionType, collection ) )
				.thenAnswer( invocation -> List.of().iterator() );
		when( action.deleteOrphans() ).thenReturn( true );
		when( style.reallyDoCascade( action ) ).thenReturn( true );
		when( style.hasOrphanDelete() ).thenReturn( true );
		when( holder.isNewlyInstantiated() ).thenReturn( scenario.newlyInstantiated() );
		when( holder.wasInitialized() ).thenReturn( scenario.initialized() );
		when( persistenceContext.getCollectionEntry( holder ) ).thenReturn( collectionEntry );
		doReturn( List.of( orphan ) ).when( collectionEntry ).getOrphans( "Element", holder );
		when( holder.getQueuedOrphans( "Element" ) ).thenReturn( List.of( orphan ) );

		final var mappingMetamodel = mappingMetamodel( factory );
		when( mappingMetamodel.getCollectionDescriptor( role ) ).thenReturn( collectionPersister );
		when( collectionPersister.getElementType() ).thenReturn( elementType );
		when( collectionPersister.isCascadeDeleteEnabled() ).thenReturn( false );

		return new Fixture(
				action,
				CascadePoint.BEFORE_FLUSH,
				session,
				persister,
				root,
				false,
				() -> {
					if ( scenario.holderBacked() ) {
						verify( persistenceContext ).getCollectionHolder( collection );
					}
					else {
						verify( persistenceContext, never() ).getCollectionHolder( collection );
					}
					if ( scenario.eligibility() != CascadeTraceEvent.OrphanEligibility.ELIGIBLE ) {
						verify( persistenceContext, never() ).getCollectionEntry( holder );
						verify( collectionEntry, never() ).getOrphans( "Element", holder );
						verify( holder, never() ).getQueuedOrphans( "Element" );
					}
					else if ( scenario.initialized() ) {
						verify( persistenceContext ).getCollectionEntry( holder );
						verify( collectionEntry ).getOrphans( "Element", holder );
						verify( holder, never() ).getQueuedOrphans( "Element" );
					}
					else {
						verify( persistenceContext, never() ).getCollectionEntry( holder );
						verify( collectionEntry, never() ).getOrphans( "Element", holder );
						verify( holder ).getQueuedOrphans( "Element" );
					}
				}
		);
	}

	@SuppressWarnings("removal")
	private static Fixture componentCollectionFixture() {
		final var action = action();
		final var style = style( "STYLE_COMPONENT_COLLECTION" );
		final var associationStyle = style( "STYLE_COMPONENT_ASSOCIATION" );
		final var collectionType = mock( CollectionType.class );
		final var componentType = mock( ComponentType.class );
		final var entityType = mock( EntityType.class );
		final var collectionPersister = mock( CollectionPersister.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var root = new Object();
		final var collection = new Object();
		final var firstComponent = new Object();
		final var secondComponent = new Object();
		final var firstAssociation = new Object();
		final var secondAssociation = new Object();
		final var consumed = new ArrayList<Object>();
		final String role = "Root.components";

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new CollectionType[] { collectionType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "components" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( collection );
		when( collectionType.getRole() ).thenReturn( role );
		when( componentType.getSubtypes() ).thenReturn( new EntityType[] { entityType } );
		when( componentType.getPropertyNames() ).thenReturn( new String[] { "association" } );
		when( componentType.getCascadeStyle( 0 ) ).thenReturn( associationStyle );
		when( componentType.getOnDeleteAction( 0 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( componentType.getPropertyValues( firstComponent, session ) )
				.thenReturn( new Object[] { firstAssociation } );
		when( componentType.getPropertyValues( secondComponent, session ) )
				.thenReturn( new Object[] { secondAssociation } );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( collectionType, style ) ).thenReturn( true );
		when( action.appliesTo( entityType, associationStyle ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.AFTER_INSERT_BEFORE_DELETE, collectionType, factory ) )
				.thenReturn( true );
		when( action.cascadeNow(
				CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION,
				entityType,
				factory
		) ).thenReturn( true );
		when( style.reallyDoCascade( action ) ).thenReturn( true );
		when( associationStyle.reallyDoCascade( action ) ).thenReturn( true );
		when( action.getCascadableChildrenIterator( session, collectionType, collection ) )
				.thenAnswer( invocation -> recordingIterator(
						List.of( firstComponent, secondComponent ),
						consumed
				) );

		final var mappingMetamodel = mappingMetamodel( factory );
		when( mappingMetamodel.getCollectionDescriptor( role ) ).thenReturn( collectionPersister );
		when( collectionPersister.getElementType() ).thenReturn( componentType );
		when( collectionPersister.isCascadeDeleteEnabled() ).thenReturn( false );

		return new Fixture(
				action,
				CascadePoint.AFTER_INSERT_BEFORE_DELETE,
				session,
				persister,
				root,
				false,
				() -> {
					assertThat( consumed ).containsExactly( firstComponent, secondComponent );
					final var order = inOrder( componentType );
					order.verify( componentType ).getPropertyValues( firstComponent, session );
					order.verify( componentType ).getPropertyValues( secondComponent, session );
				}
		);
	}

	@SuppressWarnings("removal")
	private static Fixture deepMixedComponentFixture() {
		final var action = action();
		final var rootBasicStyle = style( "STYLE_ROOT_BASIC" );
		final var outerStyle = style( "STYLE_OUTER" );
		final var outerBasicStyle = style( "STYLE_OUTER_BASIC" );
		final var innerStyle = style( "STYLE_INNER" );
		final var innerBasicStyle = style( "STYLE_INNER_BASIC" );
		final var entityStyle = style( "STYLE_ENTITY" );
		final var anyStyle = style( "STYLE_ANY" );
		final var collectionStyle = style( "STYLE_COLLECTION" );
		final var missingStyle = style( "STYLE_MISSING" );
		final var ignoredStyle = style( "STYLE_IGNORED" );
		final var rootAnyStyle = style( "STYLE_ROOT_ANY" );
		final var rootBasicType = mock( Type.class );
		final var outerBasicType = mock( Type.class );
		final var innerBasicType = mock( Type.class );
		final var ignoredType = mock( Type.class );
		final var outerType = mock( ComponentType.class );
		final var innerType = mock( ComponentType.class );
		final var missingType = mock( ComponentType.class );
		final var entityType = mock( EntityType.class );
		final var anyType = mock( AnyType.class );
		final var collectionType = mock( CollectionType.class );
		final var rootAnyType = mock( AnyType.class );
		final var elementType = mock( EntityType.class );
		final var collectionPersister = mock( CollectionPersister.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var root = new Object();
		final var rootBasic = new Object();
		final var outer = new Object();
		final var outerBasic = new Object();
		final var inner = new Object();
		final var innerBasic = new Object();
		final var entity = new Object();
		final var any = new Object();
		final var collection = new Object();
		final var element = new Object();
		final var rootAny = new Object();
		final String role = "Root.outer.inner.children";

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "rootBasic", "outer", "rootAny" } );
		when( persister.getPropertyTypes() ).thenReturn( new Type[] { rootBasicType, outerType, rootAnyType } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] {
				rootBasicStyle,
				outerStyle,
				rootAnyStyle
		} );
		when( persister.getPropertyOnDeleteActions() ).thenReturn( new OnDeleteAction[] {
				OnDeleteAction.NO_ACTION,
				OnDeleteAction.NO_ACTION,
				OnDeleteAction.NO_ACTION
		} );
		when( persister.getValue( root, 0 ) ).thenReturn( rootBasic );
		when( persister.getValue( root, 1 ) ).thenReturn( outer );
		when( persister.getValue( root, 2 ) ).thenReturn( rootAny );

		when( outerType.getPropertyNames() ).thenReturn( new String[] { "outerBasic", "inner", "missing" } );
		when( outerType.getSubtypes() ).thenReturn( new Type[] { outerBasicType, innerType, missingType } );
		when( outerType.getCascadeStyle( 0 ) ).thenReturn( outerBasicStyle );
		when( outerType.getCascadeStyle( 1 ) ).thenReturn( innerStyle );
		when( outerType.getCascadeStyle( 2 ) ).thenReturn( missingStyle );
		when( outerType.getOnDeleteAction( 0 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( outerType.getOnDeleteAction( 1 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( outerType.getOnDeleteAction( 2 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( outerType.getPropertyValues( outer, session ) )
				.thenReturn( new Object[] { outerBasic, inner, null } );

		when( innerType.getPropertyNames() )
				.thenReturn( new String[] { "innerBasic", "entity", "any", "children" } );
		when( innerType.getSubtypes() )
				.thenReturn( new Type[] { innerBasicType, entityType, anyType, collectionType } );
		when( innerType.getCascadeStyle( 0 ) ).thenReturn( innerBasicStyle );
		when( innerType.getCascadeStyle( 1 ) ).thenReturn( entityStyle );
		when( innerType.getCascadeStyle( 2 ) ).thenReturn( anyStyle );
		when( innerType.getCascadeStyle( 3 ) ).thenReturn( collectionStyle );
		when( innerType.getOnDeleteAction( 0 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( innerType.getOnDeleteAction( 1 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( innerType.getOnDeleteAction( 2 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( innerType.getOnDeleteAction( 3 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( innerType.getPropertyValues( inner, session ) )
				.thenReturn( new Object[] { innerBasic, entity, any, collection } );

		when( missingType.getPropertyNames() ).thenReturn( new String[] { "ignored" } );
		when( missingType.getSubtypes() ).thenReturn( new Type[] { ignoredType } );
		when( missingType.getCascadeStyle( 0 ) ).thenReturn( ignoredStyle );
		when( missingType.getOnDeleteAction( 0 ) ).thenReturn( OnDeleteAction.NO_ACTION );

		when( collectionType.getRole() ).thenReturn( role );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( rootBasicType, rootBasicStyle ) ).thenReturn( true );
		when( action.appliesTo( outerType, outerStyle ) ).thenReturn( true );
		when( action.appliesTo( outerBasicType, outerBasicStyle ) ).thenReturn( true );
		when( action.appliesTo( innerType, innerStyle ) ).thenReturn( true );
		when( action.appliesTo( innerBasicType, innerBasicStyle ) ).thenReturn( true );
		when( action.appliesTo( entityType, entityStyle ) ).thenReturn( true );
		when( action.appliesTo( anyType, anyStyle ) ).thenReturn( true );
		when( action.appliesTo( collectionType, collectionStyle ) ).thenReturn( true );
		when( action.appliesTo( missingType, missingStyle ) ).thenReturn( true );
		when( action.appliesTo( rootAnyType, rootAnyStyle ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, entityType, factory ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, anyType, factory ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, collectionType, factory ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, elementType, factory ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, rootAnyType, factory ) ).thenReturn( true );
		when( entityStyle.reallyDoCascade( action ) ).thenReturn( true );
		when( anyStyle.reallyDoCascade( action ) ).thenReturn( true );
		when( collectionStyle.reallyDoCascade( action ) ).thenReturn( true );
		when( rootAnyStyle.reallyDoCascade( action ) ).thenReturn( true );
		when( action.getCascadableChildrenIterator( session, collectionType, collection ) )
				.thenAnswer( invocation -> List.of( element ).iterator() );

		final var mappingMetamodel = mappingMetamodel( factory );
		when( mappingMetamodel.getCollectionDescriptor( role ) ).thenReturn( collectionPersister );
		when( collectionPersister.getElementType() ).thenReturn( elementType );
		when( collectionPersister.isCascadeDeleteEnabled() ).thenReturn( false );

		return new Fixture(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				root,
				false,
				() -> {
					final var valueOrder = inOrder( persister, outerType, innerType );
					valueOrder.verify( persister ).getValue( root, 0 );
					valueOrder.verify( persister ).getValue( root, 1 );
					valueOrder.verify( outerType ).getPropertyValues( outer, session );
					valueOrder.verify( innerType ).getPropertyValues( inner, session );
					valueOrder.verify( persister ).getValue( root, 2 );
					verify( missingType, never() ).getPropertyValues( null, session );
				}
		);
	}

	private static Fixture logicalOneToOneFixture(LogicalOneToOneScenario scenario) {
		final var action = action();
		final var style = style( "STYLE_LOGICAL_ONE_TO_ONE_ORPHAN" );
		final var oneToOneType = mock( OneToOneType.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var entry = mock( EntityEntry.class );
		final var loadedValueEntry = mock( EntityEntry.class );
		final var root = new Object();
		final var loadedValue = new Object();
		final var replacement = new Object();

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persistenceContext.getEntry( root ) ).thenReturn( entry );
		when( persistenceContext.getEntry( loadedValue ) ).thenReturn( loadedValueEntry );
		when( entry.getStatus() ).thenReturn( Status.MANAGED );
		when( entry.getLoadedValue( "child" ) ).thenReturn( loadedValue );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new OneToOneType[] { oneToOneType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "child" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( replacement );
		when( oneToOneType.isLogicalOneToOne() ).thenReturn( true );
		when( oneToOneType.getForeignKeyDirection() ).thenReturn( scenario.direction() );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( oneToOneType, style ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_FLUSH, oneToOneType, factory ) ).thenReturn( true );
		when( action.deleteOrphans() ).thenReturn( true );
		when( style.hasOrphanDelete() ).thenReturn( true );
		when( style.reallyDoCascade( action ) ).thenReturn( true );

		return new Fixture(
				action,
				CascadePoint.BEFORE_FLUSH,
				session,
				persister,
				root,
				false,
				() -> {
					verify( entry ).getLoadedValue( "child" );
					verify( persistenceContext ).getEntry( loadedValue );
				}
		);
	}

	@SuppressWarnings("removal")
	private static Fixture collectionDatabaseCascadeFixture(CollectionDatabaseCascadeScenario scenario) {
		final var action = action();
		final var style = style( "STYLE_COLLECTION_DATABASE_CASCADE" );
		final var collectionType = mock( CollectionType.class );
		final var elementType = mock( EntityType.class );
		final var collectionPersister = mock( CollectionPersister.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var root = new Object();
		final var collection = new Object();
		final var element = new Object();
		final String role = "Root.children";

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new CollectionType[] { collectionType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "children" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( collection );
		when( collectionType.getRole() ).thenReturn( role );
		when( elementType.getAssociatedEntityName() ).thenReturn( "Element" );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( collectionType, style ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_FLUSH, collectionType, factory ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_FLUSH, elementType, factory ) ).thenReturn( true );
		when( action.directionAffectedByCascadeDelete() ).thenReturn( scenario.actionDirection() );
		when( action.getCascadableChildrenIterator( session, collectionType, collection ) )
				.thenAnswer( invocation -> List.of( element ).iterator() );
		when( style.reallyDoCascade( action ) ).thenReturn( true );

		final var mappingMetamodel = mappingMetamodel( factory );
		when( mappingMetamodel.getCollectionDescriptor( role ) ).thenReturn( collectionPersister );
		when( collectionPersister.getElementType() ).thenReturn( elementType );
		when( collectionPersister.isCascadeDeleteEnabled() ).thenReturn( scenario.mappingCascadeDelete() );

		return new Fixture( action, CascadePoint.BEFORE_FLUSH, session, persister, root );
	}

	private static Iterator<Object> recordingIterator(List<?> values, List<Object> consumed) {
		final var delegate = values.iterator();
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return delegate.hasNext();
			}

			@Override
			public Object next() {
				final var next = delegate.next();
				consumed.add( next );
				return next;
			}
		};
	}

	private static Fixture builtInToOneFixture(
			CascadingAction<?> action,
			CascadePoint cascadePoint,
			CascadeStyle style,
			ForeignKeyDirection direction,
			OnDeleteAction onDeleteAction) {
		final var entityType = mock( EntityType.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var factoryOptions = mock( SessionFactoryOptions.class );
		final var root = new Object();
		final var child = new Object();

		when( session.getFactory() ).thenReturn( factory );
		when( factory.getSessionFactoryOptions() ).thenReturn( factoryOptions );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new EntityType[] { entityType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "child" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() ).thenReturn( new OnDeleteAction[] { onDeleteAction } );
		when( persister.getValue( root, 0 ) ).thenReturn( child );
		when( persister.hasCascades() ).thenReturn( true );
		when( persister.hasCascadeDelete() ).thenReturn( true );
		when( persister.hasCascadePersist() ).thenReturn( true );
		when( persister.hasToOnes() ).thenReturn( true );
		when( entityType.isAssociationType() ).thenReturn( true );
		when( entityType.getForeignKeyDirection() ).thenReturn( direction );

		return new Fixture(
				castAction( action ),
				cascadePoint,
				session,
				persister,
				root,
				true
		);
	}

	private static Fixture rootFastPathFixture() {
		final var action = action();
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var root = new Object();

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new org.hibernate.type.Type[0] );
		when( persister.getPropertyNames() ).thenReturn( new String[0] );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[0] );
		when( persister.getPropertyOnDeleteActions() ).thenReturn( new OnDeleteAction[0] );
		when( action.anythingToCascade( persister ) ).thenReturn( false );

		return new Fixture( action, CascadePoint.BEFORE_MERGE, session, persister, root );
	}

	private static Fixture rootToOneFixture() {
		final var action = action();
		final var style = style( "STYLE_TO_ONE" );
		final var entityType = mock( EntityType.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var root = new Object();
		final var child = new Object();

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new EntityType[] { entityType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "child" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( child );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( entityType, style ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, entityType, factory ) ).thenReturn( true );
		when( style.reallyDoCascade( action ) ).thenReturn( true );

		return new Fixture( action, CascadePoint.BEFORE_MERGE, session, persister, root );
	}

	private static Fixture nestedComponentFixture() {
		final var action = action();
		final var componentStyle = style( "STYLE_COMPONENT" );
		final var associationStyle = style( "STYLE_ASSOCIATION" );
		final var componentType = mock( ComponentType.class );
		final var entityType = mock( EntityType.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var root = new Object();
		final var component = new Object();
		final var associated = new Object();

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new ComponentType[] { componentType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "component" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { componentStyle } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( component );
		when( componentType.getSubtypes() ).thenReturn( new EntityType[] { entityType } );
		when( componentType.getPropertyNames() ).thenReturn( new String[] { "association" } );
		when( componentType.getCascadeStyle( 0 ) ).thenReturn( associationStyle );
		when( componentType.getOnDeleteAction( 0 ) ).thenReturn( OnDeleteAction.NO_ACTION );
		when( componentType.getPropertyValues( component, session ) ).thenReturn( new Object[] { associated } );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( componentType, componentStyle ) ).thenReturn( true );
		when( action.appliesTo( entityType, associationStyle ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, entityType, factory ) ).thenReturn( true );
		when( associationStyle.reallyDoCascade( action ) ).thenReturn( true );

		return new Fixture( action, CascadePoint.BEFORE_MERGE, session, persister, root );
	}

	private static Fixture collectionElementFixture() {
		final var action = action();
		final var style = style( "STYLE_COLLECTION" );
		final var collectionType = mock( CollectionType.class );
		final var elementType = mock( EntityType.class );
		final var collectionPersister = mock( CollectionPersister.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var root = new Object();
		final var collection = new Object();
		final var first = new Object();
		final var second = new Object();
		final String role = "Root.children";

		stubNames( action );
		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new CollectionType[] { collectionType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "children" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() )
				.thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( collection );
		when( collectionType.getRole() ).thenReturn( role );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( collectionType, style ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.AFTER_INSERT_BEFORE_DELETE, collectionType, factory ) )
				.thenReturn( true );
		when( action.cascadeNow(
				CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION,
				elementType,
				factory
		) ).thenReturn( true );
		when( style.reallyDoCascade( action ) ).thenReturn( true );
		doReturn( List.of( first, second ).iterator() )
				.when( action ).getCascadableChildrenIterator( session, collectionType, collection );

		final var mappingMetamodel = mappingMetamodel( factory );
		when( mappingMetamodel.getCollectionDescriptor( role ) ).thenReturn( collectionPersister );
		when( collectionPersister.getElementType() ).thenReturn( elementType );
		return new Fixture( action, CascadePoint.AFTER_INSERT_BEFORE_DELETE, session, persister, root );
	}

	private static MappingMetamodelImplementor mappingMetamodel(SessionFactoryImplementor factory) {
		final var mappingMetamodel = mock( MappingMetamodelImplementor.class );
		when( factory.getMappingMetamodel() ).thenReturn( mappingMetamodel );
		return mappingMetamodel;
	}

	private static void stubNames(CascadingAction<?> action) {
		when( action.toString() ).thenReturn( "ACTION_TEST" );
	}

	private static CascadeStyle style(String name) {
		final var style = mock( CascadeStyle.class );
		when( style.toString() ).thenReturn( name );
		return style;
	}

	@SuppressWarnings("unchecked")
	private static CascadingAction<Object> action() {
		return mock( CascadingAction.class );
	}

	@SuppressWarnings("unchecked")
	private static CascadingAction<Object> castAction(CascadingAction<?> action) {
		return (CascadingAction<Object>) action;
	}

	private record EnhancedToOneScenario(
			String name,
			boolean entryPresent,
			boolean attributeLoaded,
			boolean performOnLazyProperty,
			boolean managedWithoutLoadedState,
			CascadeTraceEvent.LazyDecision lazyDecision,
			CascadeTraceEvent.ValueResolution valueResolution) {
		@Override
		public String toString() {
			return name;
		}
	}

	private record CollectionIterationScenario(
			String name,
			CascadingAction<?> action,
			CascadeStyle style,
			boolean initialized,
			boolean inverse,
			CascadeTraceEvent.CollectionIteratorMode iteratorMode,
			int expectedElements) {
		@Override
		public String toString() {
			return name;
		}
	}

	private record OrphanCollectionScenario(
			String name,
			boolean holderBacked,
			boolean newlyInstantiated,
			boolean initialized,
			CascadeTraceEvent.OrphanSource source,
			CascadeTraceEvent.OrphanEligibility eligibility) {
		@Override
		public String toString() {
			return name;
		}
	}

	private record LogicalOneToOneScenario(
			String name,
			ForeignKeyDirection direction,
			CascadeTraceEvent.OrphanTiming timing) {
		@Override
		public String toString() {
			return name;
		}
	}

	private record CollectionDatabaseCascadeScenario(
			String name,
			ForeignKeyDirection actionDirection,
			boolean mappingCascadeDelete,
			boolean expectedDatabaseCascade) {
		@Override
		public String toString() {
			return name;
		}
	}

	@SuppressWarnings("unchecked")
	private static PersistentCollection<Object> persistentCollection() {
		return mock( PersistentCollection.class );
	}

	private record Fixture(
			CascadingAction<Object> action,
			CascadePoint cascadePoint,
			EventSource session,
			EntityPersister persister,
			Object root,
			boolean sharedAction,
			Runnable verification) {
		Fixture(
				CascadingAction<Object> action,
				CascadePoint cascadePoint,
				EventSource session,
				EntityPersister persister,
				Object root) {
			this( action, cascadePoint, session, persister, root, false, () -> {
			} );
		}

		Fixture(
				CascadingAction<Object> action,
				CascadePoint cascadePoint,
				EventSource session,
				EntityPersister persister,
				Object root,
				boolean sharedAction) {
			this( action, cascadePoint, session, persister, root, sharedAction, () -> {
			} );
		}

		List<CascadeTraceEvent> trace() {
			final var events = new ArrayList<CascadeTraceEvent>();
			Cascade.cascade(
					action,
					cascadePoint,
					session,
					persister,
					root,
					null,
					events::add,
					CascadeEffectMode.DECISION_ONLY
			);
			verification.run();
			return events;
		}
	}
}
