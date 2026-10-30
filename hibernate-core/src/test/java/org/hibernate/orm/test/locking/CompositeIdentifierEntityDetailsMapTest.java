package org.hibernate.orm.test.locking;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.exec.internal.lock.EntityDetails;
import org.hibernate.sql.exec.internal.lock.LockingHelper;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.Type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies that follow-on identity collection retains identifier-facing map
/// operations and mutable views without calling Java identifier equality or hashing.
///
/// @author Steve Ebersole
class CompositeIdentifierEntityDetailsMapTest {

	@Test
	void collectsAndLooksUpUsingHibernateEquality() {
		final var fixture = new Fixture();
		final var first = new Identifier( 1 );
		final var equivalent = new Identifier( 1 );
		final var second = new Identifier( 2 );
		final var map = fixture.resolve( first, equivalent, second );
		assertThat( map ).hasSize( 2 );
		assertThat( map.get( first ) ).isSameAs( map.get( equivalent ) );
		assertThat( map.get( first ).key().getIdentifier() ).isSameAs( equivalent );
		assertThat( map.containsKey( new Identifier( 1 ) ) ).isTrue();
		assertThat( map.containsKey( new Identifier( 3 ) ) ).isFalse();
		assertThat( map.get( new Identifier( 3 ) ) ).isNull();
		final var keys = map.keySet().iterator();
		assertThat( keys.next() ).isSameAs( first );
		assertThat( keys.next() ).isSameAs( second );
		assertThat( keys.hasNext() ).isFalse();
		map.forEach( (id, details) -> assertThat( id ).isInstanceOf( Identifier.class ) );
		assertThat( map.values() ).containsExactly( map.get( first ), map.get( second ) );
	}

	@Test
	void keepsEntryAndKeyViewsBackedByTheMap() {
		final var fixture = new Fixture();
		final var first = new Identifier( 1 );
		final var second = new Identifier( 2 );
		final var map = fixture.resolve( first, second );
		final var entry = map.entrySet().iterator().next();
		final var original = map.get( first );
		final var replacement = map.get( second );
		assertThat( entry.getKey() ).isSameAs( first );
		assertThat( entry.setValue( replacement ) ).isSameAs( original );
		assertThat( map.get( new Identifier( 1 ) ) ).isSameAs( replacement );
		map.put( new Identifier( 1 ), original );
		assertThat( entry.getValue() ).isSameAs( original );
		assertThat( map.entrySet().contains( new SimpleEntry<>( new Identifier( 1 ), original ) ) ).isTrue();
		assertThat( map.entrySet().remove( new SimpleEntry<>( new Identifier( 1 ), replacement ) ) ).isFalse();
		assertThat( map.entrySet().remove( new SimpleEntry<>( new Identifier( 1 ), original ) ) ).isTrue();
		assertThat( map.keySet().contains( new Identifier( 2 ) ) ).isTrue();
		assertThat( map.keySet().remove( new Identifier( 2 ) ) ).isTrue();
		assertThat( map.keySet().remove( new Identifier( 2 ) ) ).isFalse();
		assertThat( map ).isEmpty();
	}

	@Test
	void supportsMutationsAndDefaultOperations() {
		final var fixture = new Fixture();
		final var first = new Identifier( 1 );
		final var map = fixture.resolve( first );
		final var details = map.get( first );
		map.putAll( fixture.resolve( new Identifier( 2 ) ) );
		assertThat( map.computeIfAbsent( new Identifier( 1 ), id -> { throw new AssertionError(); } ) ).isSameAs( details );
		map.replaceAll( (id, value) -> details );
		assertThat( map.values() ).containsOnly( details );
		assertThat( map.remove( new Identifier( 1 ) ) ).isSameAs( details );
		final var iterator = map.entrySet().iterator();
		iterator.next();
		iterator.remove();
		assertThat( map ).isEmpty();
		map.put( first, null );
		assertThat( map.entrySet().iterator().next().toString() ).isEqualTo( first + "=null" );
		assertThat( map.containsKey( new Identifier( 1 ) ) ).isTrue();
		assertThat( map.keySet().remove( new Identifier( 1 ) ) ).isTrue();
		map.put( null, details );
		assertThat( map.keySet().iterator().next() ).isNull();
		assertThat( map.remove( null ) ).isSameAs( details );
		map.put( first, details );
		map.keySet().clear();
		assertThat( map ).isEmpty();
		map.put( first, details );
		map.entrySet().clear();
		assertThat( map ).isEmpty();
		assertThat( fixture.resolve() ).isEmpty();
	}

	@Test
	void valuesViewRetainsMutationAndReplacementSemantics() {
		final var fixture = new Fixture();
		final var first = new Identifier( 1 );
		final var second = new Identifier( 2 );
		final var map = fixture.resolve( first, second );
		final var values = map.values();
		final var replacement = map.get( second );
		map.entrySet().iterator().next().setValue( replacement );
		assertThat( values ).containsExactly( replacement, replacement );
		final var iterator = values.iterator();
		iterator.next();
		iterator.remove();
		assertThat( map.containsKey( first ) ).isFalse();
		map.put( first, null );
		assertThat( values ).containsExactly( replacement, null );
		assertThat( values.remove( null ) ).isTrue();
		values.clear();
		assertThat( map ).isEmpty();
	}

	private static class Fixture {
		private final EntityPersister persister = mock( EntityPersister.class );
		private final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		private final ExecutionContext context = mock( ExecutionContext.class );

		Fixture() {
			final var type = mock( Type.class );
			when( persister.getRootEntityName() ).thenReturn( "Item" );
			when( persister.getIdentifierType() ).thenReturn( type );
			when( type.getTypeForEqualsHashCode() ).thenReturn( type );
			when( type.getHashCode( any(), any() ) ).thenAnswer( call -> call.getArgument( 0, Identifier.class ).value );
			when( type.isEqual( any(), any(), any() ) ).thenAnswer( call ->
					call.getArgument( 0, Identifier.class ).value == call.getArgument( 1, Identifier.class ).value );
			when( context.getSession() ).thenReturn( session );
			when( session.getPersistenceContext() ).thenReturn( mock( PersistenceContext.class ) );
			when( session.generateEntityKey( any(), same( persister ) ) ).thenAnswer( call ->
					new EntityKey( call.getArgument( 0 ), persister ) );
		}

		Map<Object, EntityDetails> resolve(Identifier... identifiers) {
			final List<EntityKey> keys = java.util.Arrays.stream( identifiers )
					.map( id -> session.generateEntityKey( id, persister ) ).toList();
			return LockingHelper.resolveEntityKeys( keys, context );
		}
	}

	private record Identifier(int value) {
		@Override public int hashCode() { throw new AssertionError( "Java identifier hashing" ); }
		@Override public boolean equals(Object other) { throw new AssertionError( "Java identifier equality" ); }
	}
}
