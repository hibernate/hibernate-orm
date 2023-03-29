/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory( useCollectingStatementInspector = true )
@DomainModel( annotatedClasses = {
		InheritanceQueryGroupByTest.Parent.class,
		InheritanceQueryGroupByTest.ChildOne.class,
		InheritanceQueryGroupByTest.ChildTwo.class,
		InheritanceQueryGroupByTest.MyEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16349" )
public class InheritanceQueryGroupByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildOne childOne = new ChildOne( "child_one", 1 );
			session.persist( childOne );
			session.persist( new MyEntity( 1, childOne ) );
			session.persist( new MyEntity( 2, childOne ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MyEntity" ).executeUpdate();
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	public void testGroupBy(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MyPojo myPojo = session.createQuery(
					"select new "
							+ MyPojo.class.getName()
							+ "(sum(e.amount), re) from MyEntity e join e.parent re group by re",
					MyPojo.class
			).getSingleResult();
			assertThat( myPojo.getAmount() ).isEqualTo( 3L );
			assertThat( myPojo.getParent().getName() ).isEqualTo( "child_one" );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_one_col", 2 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", 2 );
		} );
	}

	@Test
	public void testGroupByNotSelected(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Long sum = session.createQuery(
					"select sum(e.amount) from MyEntity e join e.parent re group by re",
					Long.class
			).getSingleResult();
			assertThat( sum ).isEqualTo( 3L );
			// When not selected, group by should only use the foreign key (parent_id)
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "parent_id", 2 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_one_col", 0 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", 0 );
		} );
	}

	@Test
	public void testGroupByAndOrderBy(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MyPojo myPojo = session.createQuery(
					"select new "
							+ MyPojo.class.getName()
							+ "(sum(e.amount), re) from MyEntity e join e.parent re group by re order by re",
					MyPojo.class
			).getSingleResult();
			assertThat( myPojo.getAmount() ).isEqualTo( 3L );
			assertThat( myPojo.getParent().getName() ).isEqualTo( "child_one" );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_one_col", 3 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", 3 );
		} );
	}

	@Test
	public void testGroupByAndOrderByNotSelected(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Long sum = session.createQuery(
					"select sum(e.amount) from MyEntity e join e.parent re group by re order by re",
					Long.class
			).getSingleResult();
			assertThat( sum ).isEqualTo( 3L );
			// When not selected, group by and order by should only use the foreign key (parent_id)
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "parent_id", 3 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_one_col", 0 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", 0 );
		} );
	}

	@Entity( name = "Parent" )
	@DiscriminatorColumn( name = "disc_col", discriminatorType = DiscriminatorType.INTEGER )
	public abstract static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Parent() {
		}

		public Parent(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "ChildOne" )
	@DiscriminatorValue( "1" )
	public static class ChildOne extends Parent {
		@Column( name = "child_one_col" )
		private Integer childOneProp;

		public ChildOne() {
		}

		public ChildOne(String name, Integer childOneProp) {
			super( name );
			this.childOneProp = childOneProp;
		}
	}

	@Entity( name = "ChildTwo" )
	@DiscriminatorValue( "2" )
	public static class ChildTwo extends Parent {
		@Column( name = "child_two_col" )
		private Integer childTwoProp;

		public ChildTwo() {
		}

		public ChildTwo(String name, Integer childTwoProp) {
			super( name );
			this.childTwoProp = childTwoProp;
		}
	}

	@Entity( name = "MyEntity" )
	public static class MyEntity {
		@Id
		@GeneratedValue
		private Long id;

		private Integer amount;

		@ManyToOne
		@JoinColumn( name = "parent_id" )
		private Parent parent;

		public MyEntity() {
		}

		public MyEntity(Integer amount, Parent parent) {
			this.amount = amount;
			this.parent = parent;
		}
	}

	public static class MyPojo {
		private final Long amount;

		private final Parent parent;

		public MyPojo(Long amount, Parent parent) {
			this.amount = amount;
			this.parent = parent;
		}

		public Long getAmount() {
			return amount;
		}

		public Parent getParent() {
			return parent;
		}
	}
}
