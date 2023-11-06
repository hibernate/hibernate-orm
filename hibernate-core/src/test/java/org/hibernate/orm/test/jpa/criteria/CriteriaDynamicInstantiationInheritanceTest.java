/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		CriteriaDynamicInstantiationInheritanceTest.BaseEntity.class,
		CriteriaDynamicInstantiationInheritanceTest.AddressEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17382" )
public class CriteriaDynamicInstantiationInheritanceTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.persist( new AddressEntity( "Via Roma", "Pegognaga" ) ) );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.createQuery( "delete from AddressEntity" )
				.executeUpdate() );
	}

	@Test
	public void testImplicitDynamicInstantiation(EntityManagerFactoryScope scope) {
		executeCriteriaQuery( scope, (cb, cq, root) -> cq.multiselect(
				root.get( "id" ).alias( "id" ),
				root.get( "street" ).alias( "street" ),
				root.get( "city" ).alias( "city" )
		) );
	}

	@Test
	public void testExplicitDynamicInstantiation(EntityManagerFactoryScope scope) {
		executeCriteriaQuery( scope, (cb, cq, root) -> cq.select( cb.construct(
				AddressEntity.class,
				root.get( "id" ).alias( "id" ),
				root.get( "street" ).alias( "street" ),
				root.get( "city" ).alias( "city" )
		) ) );
	}

	public interface SelectionProducer {
		void apply(CriteriaBuilder cb, CriteriaQuery<AddressEntity> cq, Root<AddressEntity> root);
	}

	private void executeCriteriaQuery(EntityManagerFactoryScope scope, SelectionProducer selectionProducer) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<AddressEntity> cq = cb.createQuery( AddressEntity.class );
			final Root<AddressEntity> root = cq.from( AddressEntity.class );
			selectionProducer.apply( cb, cq, root );
			final List<AddressEntity> list = entityManager.createQuery( cq ).getResultList();
			assertThat( list ).hasSize( 1 );
			assertThat( list.get( 0 ).getId() ).isNotNull();
			assertThat( list.get( 0 ).getStreet() ).isEqualTo( "Via Roma" );
			assertThat( list.get( 0 ).getCity() ).isEqualTo( "Pegognaga" );
		} );
	}

	@MappedSuperclass
	public static abstract class BaseEntity {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "AddressEntity" )
	public static class AddressEntity extends BaseEntity {
		private String street;
		private String city;

		public AddressEntity() {
		}

		public AddressEntity(String street, String city) {
			this.street = street;
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public String getCity() {
			return city;
		}
	}
}
