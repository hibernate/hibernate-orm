/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Tomasz Dziurko (tdziurko at gmail dot com)
 * @author Chris Cranford
 */
public class TransactionRollbackBehaviorTest extends EnversEntityManagerFactoryBasedFunctionalTest {
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
		inJPA(
				entityManager -> {
					if ( autoClear != null ) {
						entityManager.unwrap( SessionImplementor.class ).setAutoClear( autoClear );
					}

					Integer rollbackEntityId = null;
					Integer commitEntityId = null;

					entityManager.getTransaction().begin();
					IntTestEntity entity1 = new IntTestEntity( 30 );
					entityManager.persist( entity1 );
					if ( flush ) {
						entityManager.flush();
					}
					rollbackEntityId = entity1.getId();
					entityManager.getTransaction().rollback();

					entityManager.getTransaction().begin();
					IntTestEntity entity2 = new IntTestEntity( 50 );
					entityManager.persist( entity2 );
					if ( flush ) {
						entityManager.flush();
					}
					commitEntityId = entity2.getId();
					entityManager.getTransaction().commit();

					assertThat(
							getAuditReader().getRevisions( IntTestEntity.class, commitEntityId ),
							CollectionMatchers.hasSize( 1 )
					);

					assertThat(
							getAuditReader().getRevisions( IntTestEntity.class, rollbackEntityId ),
							CollectionMatchers.isEmpty()
					);
				}
		);
	}
}
