/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.flush;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TransactionRequiredException;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10877")
public class NonTransactionalDataAccessTest extends BaseCoreFunctionalTestCase {

	private String allowUpdateOperationOutsideTransaction = "true";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(
				AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION,
				allowUpdateOperationOutsideTransaction
		);
	}

	@Override
	protected void prepareTest() throws Exception {
		final MyEntity entity = new MyEntity( "entity" );
		inTransaction(
				session -> {
					session.save( entity );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from MyEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testFlushAllowingOutOfTransactionUpdateOperations() throws Exception {
		allowUpdateOperationOutsideTransaction = "true";
		rebuildSessionFactory();
		prepareTest();
		try (Session s = openSession()) {
			final MyEntity entity = (MyEntity) s.createQuery( "from MyEntity e where e.name = :n" )
					.setParameter( "n", "entity" )
					.uniqueResult();
			assertThat( entity, not( nullValue() ) );
			entity.setName( "changed" );
			session.flush();
		}
	}

	@Test
	public void testNativeQueryAllowingOutOfTransactionUpdateOperations() throws Exception {
		allowUpdateOperationOutsideTransaction = "true";
		rebuildSessionFactory();
		prepareTest();
		try (Session s = openSession()) {
			s.createSQLQuery( "delete from MY_ENTITY" ).executeUpdate();
		}
	}

	@Test(expected = TransactionRequiredException.class)
	public void testNativeQueryDisallowingOutOfTransactionUpdateOperations() throws Exception {
		allowUpdateOperationOutsideTransaction = "false";
		rebuildSessionFactory();
		prepareTest();
		try (Session s = openSession()) {
			s.createSQLQuery( "delete from MY_ENTITY" ).executeUpdate();
		}
	}

	@Test(expected = TransactionRequiredException.class)
	public void testFlushDisallowingOutOfTransactionUpdateOperations() throws Exception {
		allowUpdateOperationOutsideTransaction = "false";
		rebuildSessionFactory();
		prepareTest();
		try (Session s = openSession()) {
			final MyEntity entity = (MyEntity) s.createQuery( "from MyEntity e where e.name = :n" )
					.setParameter( "n", "entity" )
					.uniqueResult();
			assertThat( entity, not( nullValue() ) );
			entity.setName( "changed" );
			session.flush();
		}
	}

	@Test(expected = TransactionRequiredException.class)
	public void testFlushOutOfTransaction() throws Exception {
		allowUpdateOperationOutsideTransaction = "";
		rebuildSessionFactory();
		prepareTest();
		try (Session s = openSession()) {
			final MyEntity entity = (MyEntity) s.createQuery( "from MyEntity e where e.name = :n" )
					.setParameter( "n", "entity" )
					.uniqueResult();
			assertThat( entity, not( nullValue() ) );
			entity.setName( "changed" );
			session.flush();
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		@GeneratedValue
		long id;

		String name;

		public MyEntity() {
		}

		public MyEntity(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
