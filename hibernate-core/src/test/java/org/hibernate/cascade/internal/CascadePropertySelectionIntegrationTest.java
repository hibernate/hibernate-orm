/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cascade.spi.CascadePropertySelection;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadeStyles;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadingActions;
import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Integration tests for persister-owned property selection in the consolidated
/// metadata walker.
///
/// @author Steve Ebersole
class CascadePropertySelectionIntegrationTest {
	@Test
	void rootFastPathDoesNotResolvePropertySelection() {
		final var fixture = fixture();
		when( fixture.persister.hasCascadePersist() ).thenReturn( false );

		Cascade.cascade(
				CascadingActions.PERSIST,
				CascadePoint.BEFORE_INSERT_AFTER_DELETE,
				fixture.session,
				fixture.persister,
				fixture.root
		);

		verify( fixture.persister, never() ).getCascadePropertySelection( CascadingActions.PERSIST );
		verify( fixture.persister, never() ).getPropertyTypes();
	}

	@Test
	void productionTraversalVisitsSelectedPropertiesInMappingOrder() {
		assertSelectedTraversal(
				CascadingActions.PERSIST,
				CascadePoint.BEFORE_INSERT_AFTER_DELETE,
				CascadeStyles.PERSIST,
				false
		);
	}

	@ParameterizedTest(name = "{0} at {1}")
	@MethodSource("remainingBuiltInRoutes")
	void everyOtherBuiltInRouteVisitsSelectedPropertiesInMappingOrder(
			CascadingAction<?> action,
			CascadePoint cascadePoint,
			CascadeStyle selectedStyle,
			boolean componentType) {
		assertSelectedTraversal( action, cascadePoint, selectedStyle, componentType );
	}

	private static void assertSelectedTraversal(
			CascadingAction<?> action,
			CascadePoint cascadePoint,
			CascadeStyle selectedStyle,
			boolean componentType) {
		final var fixture = fixture( action, cascadePoint, selectedStyle, componentType );

		Cascade.cascade(
				action,
				cascadePoint,
				fixture.session,
				fixture.persister,
				fixture.root
		);

		final var ordered = inOrder( fixture.persister );
		ordered.verify( fixture.persister ).getValue( fixture.root, 1 );
		ordered.verify( fixture.persister ).getValue( fixture.root, 6 );
		for ( int index = 0; index < 8; index++ ) {
			if ( index != 1 && index != 6 ) {
				verify( fixture.persister, never() ).getValue( fixture.root, index );
			}
		}
	}

	@Test
	void decisionTracingRetainsTheCompleteMetadataWalk() {
		final var fixture = fixture();
		final var events = new ArrayList<CascadeTraceEvent>();

		Cascade.cascade(
				CascadingActions.PERSIST,
				CascadePoint.BEFORE_INSERT_AFTER_DELETE,
				fixture.session,
				fixture.persister,
				fixture.root,
				null,
				events::add,
				CascadeEffectMode.DECISION_ONLY
		);

		assertThat( events ).filteredOn( CascadeTraceEvent.Node.class::isInstance ).hasSize( 8 );
		verify( fixture.persister, never() ).getCascadePropertySelection( CascadingActions.PERSIST );
	}

	private static Fixture fixture() {
		return fixture(
				CascadingActions.PERSIST,
				CascadePoint.BEFORE_INSERT_AFTER_DELETE,
				CascadeStyles.PERSIST,
				false
		);
	}

	private static Fixture fixture(
			CascadingAction<?> action,
			CascadePoint cascadePoint,
			CascadeStyle selectedStyle,
			boolean componentType) {
		final var persister = mock( AbstractEntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var session = mock( EventSource.class );
		final var root = new Object();
		final var types = new Type[8];
		final var styles = new CascadeStyle[8];
		final var names = new String[8];
		for ( int index = 0; index < 8; index++ ) {
			types[index] = componentType && ( index == 1 || index == 6 )
					? componentType()
					: mock( Type.class );
			styles[index] = index == 1 || index == 6 ? selectedStyle : CascadeStyles.NONE;
			names[index] = "property" + index;
		}
		final var selection = CascadePropertySelection.determine(
				types,
				styles,
				action
		);

		when( persister.hasCascades() ).thenReturn( true );
		when( persister.hasCascadeDelete() ).thenReturn( true );
		when( persister.hasCascadePersist() ).thenReturn( true );
		when( persister.hasToOnes() ).thenReturn( true );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( types );
		when( persister.getPropertyCascadeStyles() ).thenReturn( styles );
		when( persister.getPropertyNames() ).thenReturn( names );
		when( persister.getPropertyOnDeleteActions() ).thenReturn( new OnDeleteAction[8] );
		when( persister.getCascadePropertySelection( action ) ).thenReturn( selection );

		return new Fixture( persister, session, root );
	}

	private static ComponentType componentType() {
		final var type = mock( ComponentType.class );
		when( type.isComponentType() ).thenReturn( true );
		return type;
	}

	private static Stream<Arguments> remainingBuiltInRoutes() {
		return Stream.of(
				Arguments.of(
						CascadingActions.PERSIST,
						CascadePoint.AFTER_INSERT_BEFORE_DELETE,
						CascadeStyles.PERSIST,
						false
				),
				Arguments.of(
						CascadingActions.PERSIST_ON_FLUSH,
						CascadePoint.BEFORE_FLUSH,
						CascadeStyles.PERSIST,
						false
				),
				Arguments.of(
						CascadingActions.REMOVE,
						CascadePoint.AFTER_INSERT_BEFORE_DELETE,
						CascadeStyles.DELETE,
						false
				),
				Arguments.of(
						CascadingActions.REMOVE,
						CascadePoint.BEFORE_INSERT_AFTER_DELETE,
						CascadeStyles.DELETE,
						false
				),
				Arguments.of(
						CascadingActions.MERGE,
						CascadePoint.BEFORE_MERGE,
						CascadeStyles.MERGE,
						false
				),
				Arguments.of(
						CascadingActions.REFRESH,
						CascadePoint.BEFORE_REFRESH,
						CascadeStyles.REFRESH,
						false
				),
				Arguments.of(
						CascadingActions.EVICT,
						CascadePoint.AFTER_EVICT,
						CascadeStyles.EVICT,
						false
				),
				Arguments.of(
						CascadingActions.CHECK_ON_FLUSH,
						CascadePoint.BEFORE_FLUSH,
						CascadeStyles.NONE,
						true
				)
		);
	}

	private record Fixture(
			AbstractEntityPersister persister,
			EventSource session,
			Object root) {
	}
}
