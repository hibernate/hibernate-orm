package org.hibernate.engine.internal;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import java.util.function.IntFunction;

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
		assertThat( state.omitsColumn( 0 ) ).isTrue();
		assertThat( state.getClass().getDeclaredFields() ).filteredOn( field -> !Modifier.isStatic( field.getModifiers() ) )
				.singleElement().satisfies( field -> assertThat( field.getType() ).isEqualTo( long.class ) );
		final Object[] values = new Object[64];
		assertThat( state.physicalState( values, (FilteredAssociationMapping) null ) ).isSameAs( values );
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
		assertThat( state.omitsColumn( 0 ) ).isFalse();
		final Object[] values = new Object[65];
		final var mapping = mock( FilteredAssociationMapping.class );
		when( mapping.physicalState( same( state ), same( values ), any() ) ).thenAnswer( invocation -> {
			final IntFunction<Object> keys = invocation.getArgument( 2 );
			return new Object[] { keys.apply( 0 ), keys.apply( 64 ) };
		} );
		assertThat( state.physicalState( values, mapping ) ).containsExactly( first, last );
		state.clear( 0 );
		assertThat( state.physicalState( values, mapping ) ).containsExactly( null, last );
		assertThat( state.contains( 64 ) ).isTrue();
		state.clear( 64 );
		assertThat( state.isEmpty() ).isTrue();
	}
}
