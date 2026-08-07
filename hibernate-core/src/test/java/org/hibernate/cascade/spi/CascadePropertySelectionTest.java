/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.spi;

import java.util.stream.Stream;

import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.hibernate.cascade.spi.PropertySelectionKind.ALL;
import static org.hibernate.cascade.spi.PropertySelectionKind.NONE;
import static org.hibernate.cascade.spi.PropertySelectionKind.SELECTED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable property-selection model and its conservative built-in
/// action classification.
///
/// @author Steve Ebersole
class CascadePropertySelectionTest {
	@Test
	void determinesNoneWhenNoPropertyApplies() {
		final var selection = CascadePropertySelection.determine(
				types( 4 ),
				styles( CascadeStyles.NONE, CascadeStyles.NONE, CascadeStyles.NONE, CascadeStyles.NONE ),
				CascadingActions.PERSIST
		);

		assertThat( selection.getKind() ).isEqualTo( NONE );
		assertThat( selection.getSelectedPropertyCount() ).isZero();
	}

	@Test
	void determinesAllWhenEveryPropertyApplies() {
		final var selection = CascadePropertySelection.determine(
				types( 3 ),
				styles( CascadeStyles.PERSIST, CascadeStyles.ALL, CascadeStyles.PERSIST ),
				CascadingActions.PERSIST
		);

		assertThat( selection.getKind() ).isEqualTo( ALL );
		assertThat( selection.getSelectedPropertyCount() ).isZero();
	}

	@Test
	void selectedIndexesRetainMappingOrder() {
		final var selection = CascadePropertySelection.determine(
				types( 8 ),
				styles(
						CascadeStyles.NONE,
						CascadeStyles.PERSIST,
						CascadeStyles.NONE,
						CascadeStyles.NONE,
						CascadeStyles.NONE,
						CascadeStyles.NONE,
						CascadeStyles.ALL,
						CascadeStyles.NONE
				),
				CascadingActions.PERSIST
		);

		assertThat( selection.getKind() ).isEqualTo( SELECTED );
		assertThat( selection.getSelectedPropertyCount() ).isEqualTo( 2 );
		assertThat( selection.getSelectedProperty( 0 ) ).isEqualTo( 1 );
		assertThat( selection.getSelectedProperty( 1 ) ).isEqualTo( 6 );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("builtInActionSelections")
	void determinesSelectionForEveryBuiltInAction(
			CascadingAction<?> action,
			CascadeStyle selectedStyle,
			boolean associationType) {
		final var types = types( 8 );
		if ( associationType ) {
			types[1] = associationType();
			types[6] = associationType();
		}
		final var selection = CascadePropertySelection.determine(
				types,
				styles(
						CascadeStyles.NONE,
						selectedStyle,
						CascadeStyles.NONE,
						CascadeStyles.NONE,
						CascadeStyles.NONE,
						CascadeStyles.NONE,
						selectedStyle,
						CascadeStyles.NONE
				),
				action
		);

		assertThat( selection.getKind() ).isEqualTo( SELECTED );
		assertThat( selection.getSelectedPropertyCount() ).isEqualTo( 2 );
		assertThat( selection.getSelectedProperty( 0 ) ).isEqualTo( 1 );
		assertThat( selection.getSelectedProperty( 1 ) ).isEqualTo( 6 );
	}

	@Test
	void smallOrInsufficientlySelectiveMappingsUseAll() {
		assertThat( CascadePropertySelection.determine(
				types( 4 ),
				styles( CascadeStyles.PERSIST, CascadeStyles.NONE, CascadeStyles.NONE, CascadeStyles.NONE ),
				CascadingActions.PERSIST
		).getKind() ).isEqualTo( ALL );

		assertThat( CascadePropertySelection.determine(
				types( 8 ),
				styles(
						CascadeStyles.PERSIST,
						CascadeStyles.PERSIST,
						CascadeStyles.PERSIST,
						CascadeStyles.PERSIST,
						CascadeStyles.PERSIST,
						CascadeStyles.NONE,
						CascadeStyles.NONE,
						CascadeStyles.NONE
				),
				CascadingActions.PERSIST
		).getKind() ).isEqualTo( ALL );
	}

	@Test
	void allAndNoneDoNotExposeIndexes() {
		assertThatIllegalStateException().isThrownBy( () -> CascadePropertySelection.all().getSelectedProperty( 0 ) );
		assertThatIllegalStateException().isThrownBy( () -> CascadePropertySelection.none().getSelectedProperty( 0 ) );
	}

	@Test
	void rejectsMisalignedMetadata() {
		assertThatIllegalArgumentException().isThrownBy( () -> CascadePropertySelection.determine(
				types( 2 ),
				styles( CascadeStyles.PERSIST ),
				CascadingActions.PERSIST
		) );
	}

	private static Type[] types(int count) {
		final Type[] types = new Type[count];
		for ( int i = 0; i < count; i++ ) {
			types[i] = mock( Type.class );
		}
		return types;
	}

	private static CascadeStyle[] styles(CascadeStyle... styles) {
		return styles;
	}

	private static EntityType associationType() {
		final var type = mock( EntityType.class );
		when( type.isAssociationType() ).thenReturn( true );
		return type;
	}

	private static Stream<Arguments> builtInActionSelections() {
		return Stream.of(
				Arguments.of( CascadingActions.REMOVE, CascadeStyles.DELETE, false ),
				Arguments.of( CascadingActions.REFRESH, CascadeStyles.REFRESH, false ),
				Arguments.of( CascadingActions.EVICT, CascadeStyles.EVICT, false ),
				Arguments.of( CascadingActions.MERGE, CascadeStyles.MERGE, false ),
				Arguments.of( CascadingActions.PERSIST, CascadeStyles.PERSIST, false ),
				Arguments.of( CascadingActions.PERSIST_ON_FLUSH, CascadeStyles.PERSIST, false ),
				Arguments.of( CascadingActions.CHECK_ON_FLUSH, CascadeStyles.NONE, true )
		);
	}
}
