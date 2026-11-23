package org.hibernate.orm.test.jpa.criteria.mapjoin;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Path;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies Criteria map-key paths support dereferencing entity identifiers and ordinary attributes.
///
/// @author Steve Ebersole
@JiraKey("HHH-11515")
@Jpa(annotatedClasses = {MapKeyDereferenceTest.Owner.class, MapKeyDereferenceTest.KeyEntity.class})
public class MapKeyDereferenceTest {
	@Test
	public void testDereferenceEntityMapKey(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final var firstKey = new KeyEntity();
			firstKey.id = 1L;
			firstKey.label = "first";
			final var secondKey = new KeyEntity();
			secondKey.id = 2L;
			secondKey.label = "second";
			entityManager.persist( firstKey );
			entityManager.persist( secondKey );

			final var owner = new Owner();
			owner.id = 1L;
			owner.entries.put( firstKey, 101L );
			owner.entries.put( secondKey, 202L );
			entityManager.persist( owner );
			entityManager.flush();
			entityManager.clear();

			final var builder = entityManager.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( Owner.class );
			final MapJoin<Owner, KeyEntity, Long> mapJoin = root.joinMap( "entries" );
			final Path<KeyEntity> key = mapJoin.key();
			final Path<Long> id = key.get( "id" );
			query.select( id ).orderBy( builder.asc( id ) );

			assertThat( entityManager.createQuery( query ).getResultList() ).containsExactly( 1L, 2L );

			query.where( builder.equal( key.get( "label" ), "second" ) );
			assertThat( entityManager.createQuery( query ).getResultList() ).containsExactly( 2L );
		} );
	}

	@Entity(name = "MapKeyDereferenceOwner")
	@Table(name = "criteria_map_owner")
	public static class Owner {
		@Id
		Long id;

		@ElementCollection
		@MapKeyJoinColumn(name = "key_id")
		@Column(name = "entry_value")
		Map<KeyEntity, Long> entries = new HashMap<>();
	}

	@Entity(name = "MapKeyDereferenceKey")
	@Table(name = "criteria_map_key")
	public static class KeyEntity {
		@Id
		Long id;

		String label;
	}
}
