/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;

import org.hibernate.query.sqm.PathElementException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		CriteriaSubtypeAttributesTest.ParentEntity.class,
		CriteriaSubtypeAttributesTest.ChildEntity.class,
		CriteriaSubtypeAttributesTest.AssociatedEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18569" )
public class CriteriaSubtypeAttributesTest {
	@Test
	public void testGetParentAttribute(EntityManagerFactoryScope scope) {
		// parent root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ParentEntity> cq = cb.createQuery( ParentEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			cq.where( cb.isNotNull( parent.get( "name" ) ) );
			assertResult( entityManager.createQuery( cq ).getResultList(), 2, "name", true );
		} );
		// child root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ChildEntity> parent = cq.from( ChildEntity.class );
			cq.where( cb.isNotNull( parent.get( "name" ) ) );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "name", true );
		} );
	}

	@Test
	public void testGetSubtypeAttribute(EntityManagerFactoryScope scope) {
		// parent root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ParentEntity> cq = cb.createQuery( ParentEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			try {
				cq.where( cb.isNotNull( parent.get( "age" ) ) );
				fail( "Invoking get() for a subtype attribute on a supertype root should not be allowed" );
			}
			catch (Exception e) {
				assertThat( e ).isInstanceOf( PathElementException.class ).hasMessageContaining( "Could not resolve attribute" );
			}
		} );
		// child root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ChildEntity> child = cq.from( ChildEntity.class );
			cq.where( cb.isNotNull( child.get( "age" ) ) );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "age", true );
		} );
		// parent root treat as child
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			final Root<ChildEntity> treated = cb.treat( parent, ChildEntity.class );
			cq.select( treated );
			cq.where( cb.isNotNull( treated.get( "age" ) ) );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "age", true );
		} );
	}

	@Test
	public void testJoinParentAttribute(EntityManagerFactoryScope scope) {
		// parent root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ParentEntity> cq = cb.createQuery( ParentEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			parent.join( "association" );
			assertResult( entityManager.createQuery( cq ).getResultList(), 2, "association", false );
		} );
		// child root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ChildEntity> child = cq.from( ChildEntity.class );
			child.join( "association" );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "association", false );
		} );
	}

	@Test
	public void testJoinSubtypeAttribute(EntityManagerFactoryScope scope) {
		// parent root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ParentEntity> cq = cb.createQuery( ParentEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			try {
				parent.join( "childAssociation" );
				fail( "Invoking join() for a subtype attribute on a supertype root should not be allowed" );
			}
			catch (Exception e) {
				assertThat( e ).isInstanceOf( PathElementException.class ).hasMessageContaining( "Could not resolve attribute" );
			}
		} );
		// child root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ChildEntity> child = cq.from( ChildEntity.class );
			child.join( "childAssociation" );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "childAssociation", false );
		} );
		// parent root treat as child
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			final Root<ChildEntity> treated = cb.treat( parent, ChildEntity.class );
			cq.select( treated );
			treated.join( "childAssociation" );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "childAssociation", false );
		} );
	}

	@Test
	public void testFetchParentAttribute(EntityManagerFactoryScope scope) {
		// parent root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ParentEntity> cq = cb.createQuery( ParentEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			parent.fetch( "association" );
			assertResult( entityManager.createQuery( cq ).getResultList(), 2, "association", true );
		} );
		// child root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ChildEntity> child = cq.from( ChildEntity.class );
			child.fetch( "association" );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "association", true );
		} );
	}

	@Test
	public void testFetchSubtypeAttribute(EntityManagerFactoryScope scope) {
		// parent root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ParentEntity> cq = cb.createQuery( ParentEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			try {
				parent.fetch( "childAssociation" );
				fail( "Invoking fetch() for a subtype attribute on a supertype root should not be allowed" );
			}
			catch (Exception e) {
				assertThat( e ).isInstanceOf( PathElementException.class ).hasMessageContaining( "Could not resolve attribute" );
			}
		} );
		// child root
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ChildEntity> child = cq.from( ChildEntity.class );
			child.fetch( "childAssociation" );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "childAssociation", true );
		} );
		// parent root treat as child
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ChildEntity> cq = cb.createQuery( ChildEntity.class );
			final Root<ParentEntity> parent = cq.from( ParentEntity.class );
			final Root<ChildEntity> treated = cb.treat( parent, ChildEntity.class );
			cq.select( treated );
			treated.fetch( "childAssociation" );
			assertResult( entityManager.createQuery( cq ).getResultList(), 1, "childAssociation", true );
		} );
	}

	private void assertResult(
			final List<? extends ParentEntity> result,
			int size,
			String attributeName,
			boolean initialized) {
		assertThat( result ).hasSize( size )
				.extracting( attributeName )
				.allMatch( r -> Hibernate.isInitialized( r ) || !initialized );
		if ( size == 1 ) {
			assertThat( result.get( 0 ) ).isExactlyInstanceOf( ChildEntity.class );
		}
	}

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final AssociatedEntity associated1 = new AssociatedEntity( 1L, "associated_1" );
			entityManager.persist( associated1 );
			final AssociatedEntity associated2 = new AssociatedEntity( 2L, "associated_2" );
			entityManager.persist( associated2 );
			entityManager.persist( new ChildEntity( associated1, "child", associated2, 2 ) );
			final AssociatedEntity associated3 = new AssociatedEntity( 3L, "associated_3" );
			entityManager.persist( associated3 );
			entityManager.persist( new ParentEntity( associated3, "parent" ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "ParentEntity" )
	static class ParentEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		private AssociatedEntity association;

		private String name;

		public ParentEntity() {
		}

		public ParentEntity(AssociatedEntity association, String name) {
			this.name = name;
			this.association = association;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "ChildEntity" )
	static class ChildEntity extends ParentEntity {
		@ManyToOne( fetch = FetchType.LAZY )
		private AssociatedEntity childAssociation;

		@Column( name = "age_col" )
		private Integer age;

		public ChildEntity() {
		}

		public ChildEntity(AssociatedEntity association, String name, AssociatedEntity childAssociation, Integer age) {
			super( association, name );
			this.childAssociation = childAssociation;
			this.age = age;
		}

		public AssociatedEntity getChildAssociation() {
			return childAssociation;
		}
	}

	@Entity( name = "AssociatedEntity" )
	@Table( name = "association_test" )
	static class AssociatedEntity {
		@Id
		private Long id;

		private String name;

		public AssociatedEntity() {
		}

		public AssociatedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
