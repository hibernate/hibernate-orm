package org.hibernate.jpa.test.query;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.TypedQuery;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

public class TypedQueryResultListTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class };
	}

	@Before
	public void createTestData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent1 = new Parent( 1, "usr1", null );
			Parent parent2 = new Parent( 1, "usr2", null );
			entityManager.persist( parent1 );
			entityManager.persist( parent2 );
		} );
	}

	@After
	public void dropTestData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "delete Parent" ).executeUpdate();
		} );
	}


	@Test
	@FailureExpected(
			jiraKey = "HHH-15060",
			message = "This was the exact reported case.  Even though @NotFound support is buggy, this " +
					"query is not valid for the expected results.  See `#badExpectationResultBaselineTest` " +
					"for additional discussion about why this is an incorrect expectation."
	)
	public void badExpectationResultTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists ( " +
							"	select 1 " +
							"	from Parent otherParent " +
							"	where lower(otherParent.text) like :name" +
							"		and (parent.id = otherParent.sourceParent.id or parent.number = otherParent.number)" +
							")",
					Parent.class
			);

			query.setParameter( "name", "usr1" );
			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 2 ) );
		} );
	}

	/**
	 * Adjustment to {@link #badExpectationResultTest} in terms of the results which should actually be expected
	 *
	 * @see #actualExpectationResultBaselineTest
	 */
	@Test
	public void actualExpectationResultTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists ( " +
							"	select 1 " +
							"	from Parent otherParent " +
							"	where lower(otherParent.text) like :name" +
							"		and (parent.id = otherParent.sourceParent.id or parent.number = otherParent.number)" +
							")",
					Parent.class
			);

			query.setParameter( "name", "usr1" );
			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 0 ) );
		} );
	}

	/**
	 * A baseline test for {@link #badExpectationResultTest}, with the expectation adjustment described in
	 * {@link #actualExpectationResultTest()}.
	 *
	 * Here, instead of `.id` references (which are handled specially even outside of `@NotFound`), we use
	 * non-id references, which should ultimately return the same results.
	 */
	@Test
	public void actualExpectationResultBaselineTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists ( " +
							"	select 1 " +
							"	from Parent otherParent " +
							"	where lower(otherParent.text) like :name " +
							"		and (parent.text = otherParent.sourceParent.text or parent.number = otherParent.number) " +
							")",
					Parent.class
			);

			query.setParameter( "name", "usr1" );
			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 0 ) );
		} );
	}

	/**
	 * An adjusted query showing the results wanted in the original report.
	 *
	 * Actually a series of adjusted queries, showing a few possibilities
	 */
	@Test
	public void expectedResultQueryAdjustmentTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists ( " +
							"	select 1 " +
							"	from Parent otherParent " +
							"	where lower(otherParent.text) like :name" +
							"		and (parent = otherParent.sourceParent or parent.number = otherParent.number)" +
							")",
					Parent.class
			);

			query.setParameter( "name", "usr1" );
			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 2 ) );
		} );
	}

	@Test
	public void testIt2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists (select 1 " +
							"from Parent otherParent " +
							"where lower(otherParent.text) like :name and (parent = otherParent.sourceParent or parent.number = otherParent.number))",
					Parent.class
			);

			query.setParameter( "name", "usr1" );
			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 2 ) );
		} );


	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue
		private int id;

		@Column(name = "num", nullable = false, precision = 9)
		private Integer number;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "source_fk", referencedColumnName = "id")
		@NotFound(action = NotFoundAction.IGNORE)
		private Parent sourceParent;

		@Column(name = "txt", nullable = false, length = 20)
		private String text;

		Parent() {
		}

		public Parent(Integer num, String txt, Parent source) {
			this.number = num;
			this.text = txt;
			this.sourceParent = source;
		}

		public int getId() {
			return id;
		}
	}

}
