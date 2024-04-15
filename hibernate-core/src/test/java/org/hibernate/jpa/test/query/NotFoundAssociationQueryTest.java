package org.hibernate.jpa.test.query;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.TypedQuery;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

public class NotFoundAssociationQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class };
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent1 = new Parent( 1, "usr1", null );

			Child child = new Child( 3, "Fab" );
			Parent parent2 = new Parent( 2, "usr2", child );

			entityManager.persist( child );
			entityManager.persist( parent1 );
			entityManager.persist( parent2 );
		} );
	}

	@After
	public void tearDown() {
		doInJPA( this::entityManagerFactory, entityManager -> {
					 entityManager.createQuery( "delete from Parent" ).executeUpdate();
				 }
		);
	}

	@Test
	public void testIt() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists (select 1 " +
							"from Child child " +
							"where child.parent.name = parent.name and child.parent.id = :id)",
					Parent.class
			).setParameter( "id", 2 );

			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 1 ) );
			assertThat( entityList.get( 0 ).name, is( "usr2" ) );

		} );
	}

	@Test
	public void testIt2() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists (select 1 " +
							"from Child child " +
							"where child.name = parent.child.name )",
					Parent.class
			);

			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 1 ) );
			assertThat( entityList.get( 0 ).name, is( "usr2" ) );

		} );
	}

	@Test
	public void testIt3() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where parent.child.id = (select child.id " +
							"from Child child " +
							"where parent.child.id = :id)",
					Parent.class
			).setParameter( "id", 3 );

			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 1 ) );
			assertThat( entityList.get( 0 ).name, is( "usr2" ) );

		} );
	}

	@Test
	public void testIt4() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists (select 1 " +
							"from Child child " +
							"where parent.child.id = :id)",
					Parent.class
			).setParameter( "id", 3 );

			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 1 ) );
			assertThat( entityList.get( 0 ).name, is( "usr2" ) );

		} );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Integer id;

		@ManyToOne
		@JoinColumn(name = "source_fk", referencedColumnName = "id")
		@NotFound(action = NotFoundAction.IGNORE)
		private Child child;

		private String name;

		Parent() {
		}

		public Parent(Integer id, String name, Child child) {
			this.id = id;
			this.name = name;
			this.child = child;
		}

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "child")
		@NotFound(action = NotFoundAction.IGNORE)
		private Parent parent;

		private String name;

		public Child() {
		}

		public Child(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
