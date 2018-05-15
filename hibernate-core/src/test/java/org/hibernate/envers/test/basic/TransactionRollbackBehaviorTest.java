/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class TransactionRollbackBehaviorTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IntTestEntity.class };
	}

	@DynamicTest
	@Disabled("NYI - Requires DynamicMapMode EntityNameResolver which is not yet being registered.")
	public void testRollbackWithAutoClear() {
		testRollbackBehavior( false, true );
	}

	@DynamicTest
	public void testRollbackWithNoAutoClear() {
		testRollbackBehavior( false, false );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8189")
	public void testRollbackWithFlush() {
		// Use default auto-clear behavior
		testRollbackBehavior( true, null );
	}

	private void testRollbackBehavior(boolean flush, Boolean autoClear) {
		Session session = openSession();
		try {
			if ( autoClear != null ) {
				( (SessionImplementor) session ).setAutoClear( autoClear );
			}

			Integer rollbackEntityId = null;
			Integer commitEntityId = null;

			session.getTransaction().begin();
			IntTestEntity entity1 = new IntTestEntity( 30 );
			session.persist( entity1 );
			if ( flush ) {
				session.flush();
			}
			rollbackEntityId = entity1.getId();
			session.getTransaction().rollback();

			session.getTransaction().begin();
			IntTestEntity entity2 = new IntTestEntity( 50 );
			session.persist( entity2 );
			if ( flush ) {
				session.flush();
			}
			commitEntityId = entity2.getId();
			session.getTransaction().commit();

			List<Number> revisionForCommit = getAuditReader().getRevisions( IntTestEntity.class, commitEntityId );
			assertThat( revisionForCommit.size(), is( 1 ) );

			List<Number> revisionsForRollback = getAuditReader().getRevisions( IntTestEntity.class, rollbackEntityId );
			assertThat( revisionsForRollback.isEmpty(), is( true ) );
		}
		finally {
			session.close();
		}
	}
}
