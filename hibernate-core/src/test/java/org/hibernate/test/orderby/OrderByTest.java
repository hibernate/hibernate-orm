package org.hibernate.test.orderby;

import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Christian Beikov
 */
public class OrderByTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				P1.class,
				P2.class
		};
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( new P1( 1L, "abc" ) );
					session.persist( new P1( 2L, "abc" ) );
					session.persist( new P2( 3L, "def" ) );
				}
		);
	}

	@Override
	protected void cleanupTest() throws Exception {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-14351")
	public void testOrderBySqlNode() {
		doInHibernate(
				this::sessionFactory, session -> {
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
	@DiscriminatorValue( "P1" )
	public static class P1 extends Person {
		public P1() {
		}

		public P1(Long id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "P2")
	@DiscriminatorValue( "P2" )
	public static class P2 extends Person {
		public P2() {
		}

		public P2(Long id, String name) {
			super( id, name );
		}
	}
}