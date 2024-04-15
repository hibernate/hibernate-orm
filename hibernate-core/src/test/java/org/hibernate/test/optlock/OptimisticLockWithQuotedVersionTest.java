package org.hibernate.test.optlock;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Version;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OptimisticLockWithQuotedVersionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Person person = new Person( "1", "Fabiana" );
					session.persist( person );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	public void testHqlQueryWithOptimisticLock() {
		inTransaction(
				session -> {
					session.createQuery( "from Person e", Person.class )
							.setLockMode( LockModeType.OPTIMISTIC )
							.getResultList().get( 0 );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private String id;

		@Version
		@Column(name = "`version`")
		private long version;

		private String name;

		public Person() {
		}

		public Person(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
