package org.hibernate.orm.test.orderby;

import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				OrderByTest.Person.class,
				OrderByTest.P1.class,
				OrderByTest.P2.class
		}
)
@SessionFactory
public class OrderByTest {

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new P1( 1L, "abc" ) );
					session.persist( new P1( 2L, "abc" ) );
					session.persist( new P2( 3L, "def" ) );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14351")
	public void testOrderBySqlNode(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Person> list = session.createQuery( "from Person p order by type(p) desc, p.id", Person.class )
							.getResultList();
					assertEquals( 3L, list.get( 0 ).getId().longValue() );
					assertEquals( 1L, list.get( 1 ).getId().longValue() );
					assertEquals( 2L, list.get( 2 ).getId().longValue() );
				}
		);
	}

	@Entity(name = "Person")
	public static abstract class Person {
		@Id
		private Long id;
		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "P1")
	@DiscriminatorValue("P1")
	public static class P1 extends Person {
		public P1() {
		}

		public P1(Long id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "P2")
	@DiscriminatorValue("P2")
	public static class P2 extends Person {
		public P2() {
		}

		public P2(Long id, String name) {
			super( id, name );
		}
	}
}