/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TransactionRequiredException;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10877")
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
					session.persist( entity );
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
			s.createNativeQuery( "delete from MY_ENTITY" ).executeUpdate();
		}
	}

	@Test(expected = TransactionRequiredException.class)
	public void testNativeQueryDisallowingOutOfTransactionUpdateOperations() throws Exception {
		allowUpdateOperationOutsideTransaction = "false";
		rebuildSessionFactory();
		prepareTest();
		try (Session s = openSession()) {
			s.createNativeQuery( "delete from MY_ENTITY" ).executeUpdate();
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

	@Test
	public void hhh17743Test() throws Exception {
		allowUpdateOperationOutsideTransaction = "true";
		rebuildSessionFactory();
		prepareTest();

		try(Session s = openSession();
			EntityManager entityManager = s.getEntityManagerFactory().createEntityManager();) {

			MyEntity entity = new MyEntity("N1");
			entityManager.persist(entity);

			var q = entityManager.createNamedQuery("deleteByName");
			q.setParameter("name", "N1");
			int d = q.executeUpdate();
			assertEquals(0, d);
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	@NamedQuery(name = "deleteByName", query = "delete from MyEntity where name = :name")
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
