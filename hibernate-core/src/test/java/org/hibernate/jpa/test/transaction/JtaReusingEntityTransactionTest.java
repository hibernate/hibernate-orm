/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class JtaReusingEntityTransactionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.JPA_TRANSACTION_TYPE, "JTA" );
	}

	@Test
	public void entityTransactionShouldBeReusableTest() {
		EntityManager em = createEntityManager();
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			em.persist( new TestEntity() );
			transaction.begin();
			transaction.commit();
			transaction.begin();
			em.persist( new TestEntity() );
			transaction.commit();
		}
		finally {
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}
			em.close();
		}
		em = createEntityManager();
		try {
			transaction = em.getTransaction();
			transaction.begin();
			List<TestEntity> results = em.createQuery( "from TestEntity" ).getResultList();
			assertThat( results.size(), is( 2 ) );
			transaction.commit();
		}
		finally {
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}
			em.close();
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;
	}
}

