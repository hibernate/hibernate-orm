package org.hibernate.orm.test.mapping.inheritance.discriminator;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.criteria.JoinType.LEFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/// Adapts the HHH-14069 reporter's concrete-parent reproducer, retaining the
/// inherited association and sibling collections without explicit target entities.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		ConcreteParentLazyCollectionDiscriminatorTest.ParentEntity.class,
		ConcreteParentLazyCollectionDiscriminatorTest.Child1.class,
		ConcreteParentLazyCollectionDiscriminatorTest.Child2.class,
		ConcreteParentLazyCollectionDiscriminatorTest.SomeEntity.class
})
@SessionFactory
@JiraKey("HHH-14069")
public class ConcreteParentLazyCollectionDiscriminatorTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ParentEntity" ).executeUpdate();
			session.createMutationQuery( "delete from SomeEntity" ).executeUpdate();
		} );
	}

	@Test
	void testLazyCollectionSizes(SessionFactoryScope scope) {
		reproduce( scope, false );
	}

	@Test
	void testTypedIterationAfterInitializingSibling(SessionFactoryScope scope) {
		reproduce( scope, true );
	}

	private void reproduce(SessionFactoryScope scope, boolean iterateSecondCollection) {
		final Long ownerId = scope.fromTransaction( session -> {
			SomeEntity entity = new SomeEntity();
			Child1 child1 = new Child1();
			Child2 child2 = new Child2();
			child1.setSomeEntity( entity );
			child2.setSomeEntity( entity );
			session.persist( entity );
			session.persist( child1 );
			session.persist( child2 );
			session.flush();
			return entity.getId();
		} );

		scope.inTransaction( session -> {
			var builder = session.getCriteriaBuilder();
			var query = builder.createQuery( SomeEntity.class );
			var root = query.from( SomeEntity.class );
			root.fetch( "oneChildren", LEFT );
			root.fetch( "twoChildren", LEFT );
			query.where( builder.equal( root.get( "id" ), ownerId ) );
			SomeEntity entity = session.createQuery( query ).getSingleResult();
			assertEquals( 1, entity.getOneChildren().size() );
			assertEquals( 1, entity.getTwoChildren().size() );

			session.clear();
			entity = session.find( SomeEntity.class, ownerId );
			assertFalse( Hibernate.isInitialized( entity.getOneChildren() ) );
			assertFalse( Hibernate.isInitialized( entity.getTwoChildren() ) );
			assertEquals( 1, entity.getOneChildren().size() );
			assertInstanceOf( Child1.class, entity.getOneChildren().iterator().next() );
			assertFalse( Hibernate.isInitialized( entity.getTwoChildren() ) );

			if ( iterateSecondCollection ) {
				int count = 0;
				for ( Child2 child : entity.getTwoChildren() ) {
					assertInstanceOf( Child2.class, child );
					assertEquals( ownerId, child.getSomeEntity().getId() );
					count++;
				}
				assertEquals( 1, count );
			}
			else {
				assertEquals( 1, entity.getTwoChildren().size() );
				assertInstanceOf( Child2.class, entity.getTwoChildren().iterator().next() );
			}
		} );
	}

	@Entity(name = "ParentEntity")
	@Table(name = "CHILDREN")
	@DiscriminatorColumn(name = "TYP")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class ParentEntity {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "ENTITY_ID")
		private SomeEntity someEntity;

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		public Long getId() {
			return id;
		}

		public SomeEntity getSomeEntity() {
			return someEntity;
		}

		public void setSomeEntity(SomeEntity someEntity) {
			this.someEntity = someEntity;
		}
	}

	@Entity(name = "Child1")
	@DiscriminatorValue("1")
	public static class Child1 extends ParentEntity {
	}

	@Entity(name = "Child2")
	@DiscriminatorValue("2")
	public static class Child2 extends ParentEntity {
	}

	@Entity(name = "SomeEntity")
	public static class SomeEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@OneToMany(mappedBy = "someEntity", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Child1> oneChildren;

		@OneToMany(mappedBy = "someEntity", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Child2> twoChildren;

		public Long getId() {
			return id;
		}

		public Set<Child1> getOneChildren() {
			if ( oneChildren == null ) {
				oneChildren = new HashSet<>();
			}
			return oneChildren;
		}

		public Set<Child2> getTwoChildren() {
			if ( twoChildren == null ) {
				twoChildren = new HashSet<>();
			}
			return twoChildren;
		}
	}
}
