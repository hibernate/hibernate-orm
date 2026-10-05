package org.hibernate.orm.test.jpa.criteria;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.criteria.MapJoin;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Criteria TREAT must resolve same-named map-value attributes against the requested subtype.
///
/// @author Steve Ebersole
@Jpa(annotatedClasses = {
		TreatMapJoinAttributeTypeTest.Owner.class,
		TreatMapJoinAttributeTypeTest.Key.class,
		TreatMapJoinAttributeTypeTest.Value.class,
		TreatMapJoinAttributeTypeTest.StringValue.class,
		TreatMapJoinAttributeTypeTest.ReferenceValue.class
})
@JiraKey("HHH-10488")
class TreatMapJoinAttributeTypeTest {
	@BeforeEach
	void populate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			var key = new Key();
			key.id = 1L;
			entityManager.persist( key );
			var target = new Owner();
			target.id = 3L;
			entityManager.persist( target );
			var text = new StringValue();
			text.id = 1L;
			text.value = "test";
			entityManager.persist( text );
			var reference = new ReferenceValue();
			reference.id = 2L;
			reference.value = target;
			entityManager.persist( reference );
			for ( var value : new Value[] { text, reference } ) {
				var owner = new Owner();
				owner.id = value.id;
				owner.properties.put( key, value );
				entityManager.persist( owner );
			}
		} );
	}

	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	void testStringAttribute(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Owner.class );
			var root = query.from( Owner.class );
			MapJoin<Owner, Key, Value> map = root.joinMap( "properties" );
			var treated = builder.treat( map, StringValue.class );
			assertThat( treated.get( "value" ).getJavaType() ).isEqualTo( String.class );
			query.select( root ).where(
					builder.equal( treated.key(), entityManager.find( Key.class, 1L ) ),
					builder.equal( treated.get( "value" ), "test" ) );
			assertThat( entityManager.createQuery( query ).getResultList() ).extracting( owner -> owner.id )
					.containsExactly( 1L );
		} );
	}

	@Test
	void testReferenceAttribute(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Owner.class );
			var root = query.from( Owner.class );
			MapJoin<Owner, Key, Value> map = root.joinMap( "properties" );
			var treated = builder.treat( map, ReferenceValue.class );
			assertThat( treated.get( "value" ).getJavaType() ).isEqualTo( Owner.class );
			query.select( root ).where(
					builder.equal( treated.key(), entityManager.find( Key.class, 1L ) ),
					builder.equal( treated.get( "value" ), entityManager.find( Owner.class, 3L ) ) );
			assertThat( entityManager.createQuery( query ).getResultList() ).extracting( owner -> owner.id )
					.containsExactly( 2L );
		} );
	}

	@Entity(name = "TreatMapOwner")
	public static class Owner {
		@Id
		Long id;
		@ManyToMany
		@MapKeyJoinColumn(name = "property_key_id")
		Map<Key, Value> properties = new HashMap<>();
	}

	@Entity(name = "TreatMapKey")
	public static class Key {
		@Id
		Long id;
	}

	@Entity(name = "TreatMapValue")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Value {
		@Id
		Long id;
	}

	@Entity(name = "TreatMapStringValue")
	public static class StringValue extends Value {
		@Column(name = "text_value")
		String value;
	}

	@Entity(name = "TreatMapReferenceValue")
	public static class ReferenceValue extends Value {
		@ManyToOne
		Owner value;
	}
}
