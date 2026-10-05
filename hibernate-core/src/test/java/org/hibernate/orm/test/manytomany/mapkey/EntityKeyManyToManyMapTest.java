package org.hibernate.orm.test.manytomany.mapkey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests entity keys in a bidirectional many-to-many map without an association entity.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		EntityKeyManyToManyMapTest.MapOwner.class,
		EntityKeyManyToManyMapTest.MapKeyEntity.class,
		EntityKeyManyToManyMapTest.MapValue.class
})
@SessionFactory
@JiraKey("HHH-12459")
class EntityKeyManyToManyMapTest {
	@BeforeEach
	void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var key = new MapKeyEntity();
			key.id = 1L;
			session.persist( key );
			var value = new MapValue();
			value.id = 2L;
			value.name = "shared value";
			for ( long id = 3L; id <= 4L; id++ ) {
				var owner = new MapOwner();
				owner.id = id;
				owner.values.put( key, value );
				value.owners.add( owner );
				session.persist( owner );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testCascadeAndInverseAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var key = session.find( MapKeyEntity.class, 1L );
			var value = session.find( MapValue.class, 2L );
			assertThat( value.name ).isEqualTo( "shared value" );
			assertThat( value.owners ).extracting( owner -> owner.id ).containsExactlyInAnyOrder( 3L, 4L );
			for ( var owner : value.owners ) {
				assertThat( owner.values ).hasSize( 1 );
				assertThat( owner.values.get( key ) ).isSameAs( value );
			}
		} );
	}

	@Test
	void testRemoveAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var owner = session.find( MapOwner.class, 3L );
			var key = session.find( MapKeyEntity.class, 1L );
			var value = owner.values.remove( key );
			assertThat( value ).isNotNull();
			value.owners.remove( owner );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( MapOwner.class, 3L ).values ).isEmpty();
			var value = session.find( MapValue.class, 2L );
			assertThat( value ).isNotNull();
			assertThat( value.owners ).extracting( owner -> owner.id ).containsExactly( 4L );
			var key = session.find( MapKeyEntity.class, 1L );
			assertThat( session.find( MapOwner.class, 4L ).values.get( key ) ).isSameAs( value );
		} );
	}

	@Entity(name = "EntityKeyMapOwner")
	public static class MapOwner {
		@Id
		Long id;

		@ManyToMany(cascade = CascadeType.PERSIST)
		@JoinTable(name = "entity_key_map_entries",
				joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "value_id"))
		@MapKeyJoinColumn(name = "key_id")
		Map<MapKeyEntity, MapValue> values = new HashMap<>();
	}

	@Entity(name = "EntityKeyMapKey")
	public static class MapKeyEntity {
		@Id
		Long id;
	}

	@Entity(name = "EntityKeyMapValue")
	public static class MapValue {
		@Id
		Long id;

		String name;

		@ManyToMany(mappedBy = "values")
		Set<MapOwner> owners = new HashSet<>();
	}
}
