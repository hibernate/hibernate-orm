/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.ArrayList;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests normalized cascade diagnostics, disabled tracing, and failure-path
/// capture.
///
/// @author Steve Ebersole
class CascadeDiagnosticsTest {
	@Test
	void disabledTraceDoesNotRequireADiagnosticSinkOnFailure() {
		final var action = action();
		final var style = mock( CascadeStyle.class );
		final var entityType = mock( EntityType.class );
		final var persister = mock( EntityPersister.class );
		final var session = mock( EventSource.class );
		final var root = new Object();
		final var failure = new IllegalStateException( "expected failure" );
		final var context = new CascadeTraversalContext<>(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				root,
				null
		);

		when( persister.getEntityName() ).thenReturn( "Root" );
		context.pushPath( "component" );
		context.setCurrentProperty( root, "association", entityType, style, 0 );

		assertThat( context.traceEnabled() ).isFalse();
		context.attachFailurePath( failure );

		assertThat( failure.getSuppressed() ).singleElement().isInstanceOfSatisfying(
				CascadePathDiagnostic.class,
				diagnostic -> assertThat( diagnostic.path() )
						.containsExactly( "component", "association" )
		);
	}

	@Test
	void failureTraceCapturesDeepestPathBeforeExceptionalStateRestoration() {
		final var action = action();
		final var style = mock( CascadeStyle.class );
		final var entityType = mock( EntityType.class );
		final var persister = mock( EntityPersister.class );
		final var session = mock( EventSource.class );
		final var root = new Object();
		final var failure = new IllegalStateException( "expected failure" );
		final var events = new ArrayList<CascadeTraceEvent>();
		final var context = new CascadeTraversalContext<>(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				root,
				null,
				events::add
		);

		when( persister.getEntityName() ).thenReturn( "Root" );
		context.pushPath( "component" );
		context.setCurrentProperty( root, "association", entityType, style, 0 );
		context.traceFailure( failure );

		context.popPath();
		context.setCurrentProperty( root, null, null, null, -1 );
		context.traceFailure( failure );
		context.attachFailurePath( failure );
		context.attachFailurePath( failure );

		assertThat( events ).singleElement().isInstanceOfSatisfying(
				CascadeTraceEvent.Failure.class,
				event -> {
					assertThat( event.location().rootEntityName() ).isEqualTo( "Root" );
					assertThat( event.location().path() )
							.containsExactly( "component", "association" );
					assertThat( event.exceptionType() ).isEqualTo( IllegalStateException.class.getName() );
					assertThat( event.message() ).isEqualTo( "expected failure" );
				}
		);
		assertThat( failure.getSuppressed() ).singleElement().isInstanceOfSatisfying(
				CascadePathDiagnostic.class,
				diagnostic -> {
					assertThat( diagnostic.rootEntityName() ).isEqualTo( "Root" );
					assertThat( diagnostic.path() )
							.containsExactly( "component", "association" );
					assertThat( diagnostic.getStackTrace() ).isEmpty();
				}
		);
		assertThat( context.path() ).isNull();
		assertThat( context.currentPropertyName() ).isNull();
	}

	@Test
	void traceReportsPathWorkspaceAllocations() {
		final var action = action();
		final var persister = mock( EntityPersister.class );
		final var session = mock( EventSource.class );
		final var allocations = new int[1];
		final var context = new CascadeTraversalContext<>(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				new Object(),
				null,
				new CascadeDecisionTrace() {
					@Override
					public void record(CascadeTraceEvent event) {
					}

					@Override
					public void pathAllocated() {
						allocations[0]++;
					}
				}
		);

		context.pushPath( "component" );
		context.pushPath( "association" );
		assertThat( allocations[0] ).isEqualTo( 1 );

		context.popPath();
		context.popPath();
		context.pushPath( "collection" );
		assertThat( allocations[0] ).isEqualTo( 2 );
	}

	@Test
	void decisionOnlyRecordsSelectedActionWithoutExecutingIt() {
		final var action = action();
		final var style = mock( CascadeStyle.class );
		final var entityType = mock( EntityType.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var root = new Object();
		final var child = new Object();
		final var events = new ArrayList<CascadeTraceEvent>();

		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new EntityType[] { entityType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "child" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() ).thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( child );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( entityType, style ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, entityType, factory ) ).thenReturn( true );
		when( style.reallyDoCascade( action ) ).thenReturn( true );

		Cascade.cascade(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				root,
				null,
				events::add,
				CascadeEffectMode.DECISION_ONLY
		);

		final var actionEvent = assertEvent( events, events.size() - 1, CascadeTraceEvent.Action.class );
		assertThat( actionEvent.decision() )
				.isEqualTo( CascadeTraceEvent.ActionDecision.SUPPRESSED_BY_DECISION_ONLY );
		verify( persister ).getValue( root, 0 );
		verify( action, never() ).cascade(
				session,
				child,
				null,
				"Root",
				"child",
				null,
				null,
				false
		);
		verify( persistenceContext, never() ).addChildParent( child, root );
	}

	@Test
	void tracesSelectedToOneInSemanticOrder() {
		final var action = action();
		final var style = mock( CascadeStyle.class );
		final var entityType = mock( EntityType.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var root = new Object();
		final var child = new Object();
		final var events = new ArrayList<CascadeTraceEvent>();

		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new EntityType[] { entityType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "child" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( persister.getPropertyOnDeleteActions() ).thenReturn( new OnDeleteAction[] { OnDeleteAction.NO_ACTION } );
		when( persister.getValue( root, 0 ) ).thenReturn( child );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( entityType, style ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, entityType, factory ) ).thenReturn( true );
		when( style.reallyDoCascade( action ) ).thenReturn( true );
		when( entityType.getAssociatedEntityName() ).thenReturn( "Child" );
		when( entityType.getForeignKeyDirection() ).thenReturn( ForeignKeyDirection.FROM_PARENT );
		Cascade.cascade(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				root,
				null,
				events::add
		);

		assertThat( events ).hasSize( 6 );
		assertThat( events.get( 0 ) )
				.isEqualTo( new CascadeTraceEvent.Root(
						location( action, CascadePoint.BEFORE_MERGE ),
						CascadeTraceEvent.RootDecision.TRAVERSE
				) );

		final var node = assertEvent( events, 1, CascadeTraceEvent.Node.class );
		assertChildLocation( node.location(), action, style, CascadePoint.BEFORE_MERGE );
		assertThat( node.applies() ).isTrue();
		assertThat( node.databaseCascade() ).isFalse();

		final var lazy = assertEvent( events, 2, CascadeTraceEvent.Lazy.class );
		assertThat( lazy.decision() ).isEqualTo( CascadeTraceEvent.LazyDecision.LOADED );
		final var value = assertEvent( events, 3, CascadeTraceEvent.Value.class );
		assertThat( value.resolution() ).isEqualTo( CascadeTraceEvent.ValueResolution.ENTITY_PROPERTY );
		final var association = assertEvent( events, 4, CascadeTraceEvent.Association.class );
		assertThat( association.traversed() ).isTrue();
		final var invocation = assertEvent( events, 5, CascadeTraceEvent.Action.class );
		assertThat( invocation.decision() ).isEqualTo( CascadeTraceEvent.ActionDecision.INVOKED );
		assertThat( invocation.databaseCascade() ).isFalse();

		for ( int i = 2; i < events.size(); i++ ) {
			assertChildLocation( events.get( i ).location(), action, style, CascadePoint.BEFORE_MERGE );
		}
		verify( persistenceContext ).removeChildParent( child );
	}

	private static CascadeTraceEvent.Location location(
			CascadingAction<?> action,
			CascadePoint cascadePoint) {
		return new CascadeTraceEvent.Location(
				"Root",
				java.util.List.of(),
				CascadeTraceEvent.NodeKind.ROOT,
				"",
				action.toString(),
				cascadePoint
		);
	}

	private static void assertChildLocation(
			CascadeTraceEvent.Location location,
			CascadingAction<?> action,
			CascadeStyle style,
			CascadePoint cascadePoint) {
		assertThat( location.rootEntityName() ).isEqualTo( "Root" );
		assertThat( location.path() ).containsExactly( "child" );
		assertThat( location.nodeKind() ).isEqualTo( CascadeTraceEvent.NodeKind.TO_ONE );
		assertThat( location.cascadeStyle() ).isEqualTo( style.toString() );
		assertThat( location.action() ).isEqualTo( action.toString() );
		assertThat( location.cascadePoint() ).isEqualTo( cascadePoint );
	}

	private static <E extends CascadeTraceEvent> E assertEvent(
			ArrayList<CascadeTraceEvent> events,
			int index,
			Class<E> eventType) {
		assertThat( events.get( index ) ).isInstanceOf( eventType );
		return eventType.cast( events.get( index ) );
	}

	@SuppressWarnings("unchecked")
	private static CascadingAction<Object> action() {
		return mock( CascadingAction.class );
	}
}
