/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilteredAssociationStateTest {
	@Test
	void singleWordDoesNotRetainAnyKeys() throws Exception {
		final var state = FilteredAssociationState.create( 64, false );
		final Object key = new Object();
		state.set( 0, key );
		state.set( 63, key );
		assertThat( state.contains( 0 ) ).isTrue();
		assertThat( state.contains( 62 ) ).isFalse();
		assertThat( state.contains( 63 ) ).isTrue();
		assertThat( state.retainsKeys() ).isFalse();
		assertThat( state.getClass().getDeclaredFields() ).filteredOn( field -> !Modifier.isStatic( field.getModifiers() ) )
				.singleElement().satisfies( field -> assertThat( field.getType() ).isEqualTo( long.class ) );
		assertThatThrownBy( () -> state.key( 0 ) ).isInstanceOf( IllegalStateException.class );
		state.clear( 0 );
		assertThat( state.isEmpty() ).isFalse();
		state.clear( 63 );
		assertThat( state.isEmpty() ).isTrue();
	}

	@Test
	void wordBoundariesDoNotAliasAndDoNotRetainKeys() {
		final var state = FilteredAssociationState.create( 130, false );
		for ( int slot : new int[] { 0, 63, 64, 65, 127, 128, 129 } ) {
			state.set( slot, new Object() );
		}
		state.clear( 0 );
		state.clear( 64 );
		state.clear( 128 );
		assertThat( state.contains( 0 ) ).isFalse();
		assertThat( state.contains( 64 ) ).isFalse();
		assertThat( state.contains( 128 ) ).isFalse();
		assertThat( state.contains( 63 ) ).isTrue();
		assertThat( state.contains( 65 ) ).isTrue();
		assertThat( state.contains( 127 ) ).isTrue();
		assertThat( state.contains( 129 ) ).isTrue();
		assertThat( state.getClass().getDeclaredFields() ).filteredOn( field -> !Modifier.isStatic( field.getModifiers() ) )
				.singleElement().satisfies( field -> assertThat( field.getType() ).isEqualTo( long[].class ) );
		for ( int slot : new int[] { 63, 65, 127, 129 } ) {
			state.clear( slot );
		}
		assertThat( state.isEmpty() ).isTrue();
	}

	@Test
	void exceptionalMappingsRetainAndReleaseOriginalKeys() {
		final var state = FilteredAssociationState.create( 65, true );
		final Object first = new Object();
		final Object last = new Object();
		state.set( 0, first );
		state.set( 64, last );
		assertThat( state.retainsKeys() ).isTrue();
		assertThat( state.key( 0 ) ).isSameAs( first );
		assertThat( state.key( 64 ) ).isSameAs( last );
		state.clear( 0 );
		assertThat( state.key( 0 ) ).isNull();
		assertThat( state.contains( 64 ) ).isTrue();
		state.clear( 64 );
		assertThat( state.isEmpty() ).isTrue();
	}
}
