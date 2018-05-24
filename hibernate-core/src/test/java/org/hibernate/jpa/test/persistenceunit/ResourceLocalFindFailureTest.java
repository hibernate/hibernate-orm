/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.persistenceunit;

import java.util.Collections;
import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.SharedCacheMode;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Should be able to begin a transaction after EM#find failure
 */
public class ResourceLocalFindFailureTest {

	@Test
	@TestForIssue(jiraKey = "HHH-12622")
	public void testIgnoreRollbackOnlyCall() {
		final Map<Object, Object> config = Environment.getProperties();
		config.put( "transaction-type", "RESOURCE_LOCAL" );
		testIt( config );
	}

	private void testIt(Map config) {
		EntityManagerFactoryBuilder entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
				config
		);
		EntityManager entityManager = entityManagerFactoryBuilder.build().createEntityManager();
		try {
			try {
				entityManager.find( String.class, 1 );
				fail( "entityManager.find should of thrown IAE" );
			}
			catch (IllegalArgumentException expected) {
				// ignore
			}
			catch (Throwable unexpected) {
				fail( "entityManager.find threw unexpected exception " + unexpected.getMessage() );
			}
			EntityTransaction entityTransaction = entityManager.getTransaction();
			try {
				entityTransaction.begin();
				entityTransaction.rollback();
			}
			catch (Exception unexpected) {
				fail( "entityTransaction#begin failed with " + unexpected.getMessage() );
			}
		}
		finally {
			entityManager.getEntityManagerFactory().close();
			entityManager.close();
		}
	}


	@Cacheable
	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		private Long id;
	}
}
