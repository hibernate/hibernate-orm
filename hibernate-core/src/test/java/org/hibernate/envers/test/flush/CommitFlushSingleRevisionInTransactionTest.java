/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.flush;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Disabled("Technically this test is no longer valid as the #getCurrentRevision method was removed in 6.0")
public class CommitFlushSingleRevisionInTransactionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;
	private int revisionNumberBeforeFlush;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-11575")
	public void testSingleRevisionInTransaction() {
//		// Perform audit operations
//		entityManagerFactoryScope().inTransactionsWithInit(
//				// Init callback
//				entityManager -> entityManager.unwrap( Session.class ).setFlushMode( FlushMode.COMMIT ),
//
//				// Revision 1
//				entityManager -> {
//					SequenceIdRevisionEntity revisionBeforeFlush = getAuditReader().getCurrentRevision( SequenceIdRevisionEntity.class, true );
//					revisionNumberBeforeFlush = revisionBeforeFlush.getId();
//
//					entityManager.flush();
//
//					StrTestEntity entity = new StrTestEntity( "entity" );
//					entityManager.persist( entity );
//
//					this.id = entity.getId();
//				}
//		);
//
//		final Object result = getAuditReader().createQuery()
//				.forRevisionsOfEntity( StrTestEntity.class, false, false )
//				.add( AuditEntity.id().eq( id ) )
//				.getSingleResult();
//
//		// The revision number obtained prior to the flush and the persisting of the entity should be the
//		// same as the revision number of the entity because there should only be one revision generated
//		// per transaction.
//		final SequenceIdRevisionEntity revisionEntity = (SequenceIdRevisionEntity) ( (Object[]) result )[1];
//		assertThat( revisionEntity.getId(), equalTo( revisionNumberBeforeFlush ) );
	}
}
