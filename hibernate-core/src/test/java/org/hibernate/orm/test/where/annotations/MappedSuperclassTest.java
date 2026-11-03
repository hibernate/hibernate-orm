package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				MappedSuperclassTest.Child.class,
				MappedSuperclassTest.SubClass.class,
				MappedSuperclassTest.Owner.class
		}
)
@Jira( "https://hibernate.atlassian.net/browse/HHH-21064" )
public class MappedSuperclassTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Owner owner = new Owner();
					owner.id = 1L;
					entityManager.persist( owner );

					Child child1 = new SubClass( 1L );
					child1.owner = owner;
					child1.state = 1;
					entityManager.persist( child1 );

					Child child2 = new Child( 2L );
					child2.owner = owner;
					child2.state = 0;
					entityManager.persist( child2 );

					owner.linkedChildren.add( child1 );
					owner.linkedChildren.add( child2 );
				}
		);
	}

	@Test
	public void testFindParent(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Child> children = entityManager.createQuery( "select c from Child c", Child.class )
							.getResultList();
					assertVisibleChild( children );
				}
		);
	}

	@Test
	void lazyCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> assertVisibleChild( em.find( Owner.class, 1L ).children ) );
	}

	@Test
	void fetchJoinedCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> assertVisibleChild( em.createQuery(
				"from Owner o left join fetch o.children", Owner.class ).getSingleResult().children ) );
	}

	@Test
	void manyToManyCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> assertVisibleChild( em.find( Owner.class, 1L ).linkedChildren ) );
	}

	private static void assertVisibleChild(List<Child> children) {
		assertEquals( List.of( 2L ), children.stream().map( child -> child.id ).toList() );
	}

	@Entity(name = "Owner")
	public static class Owner {
		@Id
		private Long id;

		@OneToMany(mappedBy = "owner")
		private List<Child> children;

		@ManyToMany
		private List<Child> linkedChildren = new ArrayList<>();
	}

	@Entity(name = "Child")
	public static class Child extends Intermediate {
		@Id
		private Long id;

		@ManyToOne
		private Owner owner;

		public Child() {
		}

		public Child(long id) {
			this.id = id;
		}
	}

	@Entity(name = "SubClass")
	public static class SubClass extends Child {
		public SubClass() {
		}

		public SubClass(long id) {
			super( id );
		}
	}

	@MappedSuperclass
	public static class Intermediate extends Parent {
	}

	@MappedSuperclass
	@SQLRestriction("state = 0")
	public static class Parent {
		public Parent() {
		}

		int state;
	}
}
