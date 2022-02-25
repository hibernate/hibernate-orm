package org.hibernate.jpa.test.query;

import java.util.List;
import java.util.Map;
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

import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.assertj.core.api.Assertions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

public class TypedQueryResultListTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor statementInspector;

	@Override
	protected void addConfigOptions(Map options) {
		statementInspector = new SQLStatementInterceptor( options );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class };
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent1 = new Parent( 1, "usr1", null );
			Parent parent2 = new Parent( 1, "usr2", null );
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
		statementInspector.clear();
		doInJPA( this::entityManagerFactory, entityManager -> {

			TypedQuery<Parent> query = entityManager.createQuery(
					"select parent " +
							"from Parent parent " +
							"where exists (select 1 " +
							"from Parent otherParent " +
							"where lower(otherParent.text) like :name and (parent.id = otherParent.sourceParent.id or parent.number = otherParent.number))",
					Parent.class
			);

			query.setParameter( "name", "usr1" );
			List<Parent> entityList = query.getResultList();
			assertThat( entityList.size(), is( 2 ) );

			Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " left " );
			Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
			Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " cross " );
		} );
	}

	@Test
	public void testIt2() {
		statementInspector.clear();
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

			Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
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
