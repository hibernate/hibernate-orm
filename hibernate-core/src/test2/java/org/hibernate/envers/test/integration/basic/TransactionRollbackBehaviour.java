/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.IntTestEntity;
import org.hibernate.internal.SessionImpl;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Tomasz Dziurko (tdziurko at gmail dot com)
 */
public class TransactionRollbackBehaviour extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {IntTestEntity.class};
	}

	@Test
	public void testAuditRecordsRollbackWithAutoClear() {
		testAuditRecordsRollbackBehavior( false, true );
	}

	@Test
	public void testAuditRecordsRollbackWithNoAutoClear() {
		testAuditRecordsRollbackBehavior( false, false );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8189")
	public void testFlushedAuditRecordsRollback() {
		// default auto-clear behavior
		testAuditRecordsRollbackBehavior( true, null );
	}

	private void testAuditRecordsRollbackBehavior(boolean flush, Boolean autoClear) {
		EntityManager entityManager = getEntityManager();
		try {
			if ( autoClear != null ) {
				entityManager.unwrap( SessionImpl.class ).setAutoClear( autoClear );
			}

			// persist and rollback
			entityManager.getTransaction().begin();
			IntTestEntity rollbackEntity = new IntTestEntity( 30 );
			entityManager.persist( rollbackEntity );
			if ( flush ) {
				entityManager.flush();
			}
			Integer rollbackId = rollbackEntity.getId();
			entityManager.getTransaction().rollback();

			// persist and commit
			entityManager.getTransaction().begin();
			IntTestEntity commitEntity = new IntTestEntity( 50 );
			entityManager.persist( commitEntity );
			if ( flush ) {
				entityManager.flush();
			}
			Integer commitId = commitEntity.getId();
			entityManager.getTransaction().commit();

			List<Number> revisionsForSavedClass = getAuditReader().getRevisions(
					IntTestEntity.class,
					commitId
			);
			assertEquals( "There should be one revision for inserted entity.", 1, revisionsForSavedClass.size() );

			List<Number> revisionsForRolledbackClass = getAuditReader().getRevisions(
					IntTestEntity.class,
					rollbackId
			);
			assertEquals( "There should be no revision for rolled back entity.", 0, revisionsForRolledbackClass.size() );
		}
		finally {
			entityManager.close();
		}
	}
}
