/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.IntTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Tomasz Dziurko (tdziurko at gmail dot com)
 */
@EnversTest
@Jpa(annotatedClasses = {IntTestEntity.class})
public class TransactionRollbackBehaviour {

	@Test
	public void testAuditRecordsRollbackWithAutoClear(EntityManagerFactoryScope scope) {
		testAuditRecordsRollbackBehavior( scope, false, true );
	}

	@Test
	public void testAuditRecordsRollbackWithNoAutoClear(EntityManagerFactoryScope scope) {
		testAuditRecordsRollbackBehavior( scope, false, false );
	}

	@Test
	@JiraKey(value = "HHH-8189")
	public void testFlushedAuditRecordsRollback(EntityManagerFactoryScope scope) {
		// default auto-clear behavior
		testAuditRecordsRollbackBehavior( scope, true, null );
	}

	private void testAuditRecordsRollbackBehavior(EntityManagerFactoryScope scope, boolean flush, Boolean autoClear) {
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		try {
			if ( autoClear != null ) {
				entityManager = entityManager.unwrap( Session.class )
						.sessionWithOptions().autoClear( autoClear ).openSession();
			}

			final EntityManager em = entityManager;

			// persist and rollback
			em.getTransaction().begin();
			IntTestEntity rollbackEntity = new IntTestEntity( 30 );
			em.persist( rollbackEntity );
			if ( flush ) {
				em.flush();
			}
			Integer rollbackId = rollbackEntity.getId();
			em.getTransaction().rollback();

			// persist and commit
			em.getTransaction().begin();
			IntTestEntity commitEntity = new IntTestEntity( 50 );
			em.persist( commitEntity );
			if ( flush ) {
				em.flush();
			}
			Integer commitId = commitEntity.getId();
			em.getTransaction().commit();

			List<Number> revisionsForSavedClass = AuditReaderFactory.get( em ).getRevisions(
					IntTestEntity.class,
					commitId
			);
			assertEquals( 1, revisionsForSavedClass.size(), "There should be one revision for inserted entity." );

			List<Number> revisionsForRolledbackClass = AuditReaderFactory.get( em ).getRevisions(
					IntTestEntity.class,
					rollbackId
			);
			assertEquals( 0, revisionsForRolledbackClass.size(), "There should be no revision for rolled back entity." );
		}
		finally {
			entityManager.close();
		}
	}
}
