/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		QueryToManyWithNestedToOneTest.ParentEntity.class,
		QueryToManyWithNestedToOneTest.ValueEntity.class,
		QueryToManyWithNestedToOneTest.KeyEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17857" )
public class QueryToManyWithNestedToOneTest {
	@Test
	public void testCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<ParentEntity> cq = cb.createQuery( ParentEntity.class );
			final Root<ParentEntity> root = cq.from( ParentEntity.class );
			final SetJoin<ParentEntity, ValueEntity> valuesJoin = root.joinSet( "values" );
			final Join<ValueEntity, KeyEntity> key = valuesJoin.join( "key" );
			final ParentEntity result = session.createQuery(
					cq.where( key.get( "keyValue" ).in( "key_1", "key_2" ) )
			).getSingleResult();
			assertThat( result.getValues() ).hasSize( 2 )
					.extracting( ValueEntity::getKey )
					.extracting( KeyEntity::getKeyValue )
					.containsOnly( "key_1", "key_2" );
		} );
	}

	@Test
	public void testHQLQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEntity result = session.createQuery(
					"from ParentEntity p join p.values values join values.key key where key.keyValue in ('key_3')",
					ParentEntity.class
			).getSingleResult();
			assertThat( result.getValues() ).hasSize( 1 )
					.extracting( ValueEntity::getKey )
					.extracting( KeyEntity::getKeyValue )
					.containsOnly( "key_3" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEntity parent1 = new ParentEntity();
			final ValueEntity value1 = new ValueEntity( parent1, new KeyEntity( "key_1" ) );
			session.persist( value1 );
			final ValueEntity value2 = new ValueEntity( parent1, new KeyEntity( "key_2" ) );
			session.persist( value2 );
			final ValueEntity value3 = new ValueEntity( new ParentEntity(), new KeyEntity( "key_3" ) );
			session.persist( value3 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ValueEntity" ).executeUpdate();
			session.createMutationQuery( "delete from ParentEntity" ).executeUpdate();
			session.createMutationQuery( "delete from KeyEntity" ).executeUpdate();
		} );
	}

	@Entity( name = "ParentEntity" )
	static class ParentEntity {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany( mappedBy = "parent" )
		private Set<ValueEntity> values = new HashSet<>();

		public Set<ValueEntity> getValues() {
			return values;
		}
	}

	@Entity( name = "ValueEntity" )
	static class ValueEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne( cascade = CascadeType.ALL )
		private ParentEntity parent;

		@ManyToOne( cascade = CascadeType.ALL )
		private KeyEntity key;

		public ValueEntity() {
		}

		public ValueEntity(ParentEntity parent, KeyEntity key) {
			this.parent = parent;
			this.key = key;
		}

		public KeyEntity getKey() {
			return key;
		}
	}

	@Entity( name = "KeyEntity" )
	static class KeyEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String keyValue;

		public KeyEntity() {
		}

		public KeyEntity(String keyValue) {
			this.keyValue = keyValue;
		}

		public String getKeyValue() {
			return keyValue;
		}
	}
}
