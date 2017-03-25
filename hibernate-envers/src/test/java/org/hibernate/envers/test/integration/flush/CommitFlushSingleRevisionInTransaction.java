/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.flush;

import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class CommitFlushSingleRevisionInTransaction extends AbstractFlushTest {

	@Override
	public FlushMode getFlushMode() {
		return FlushMode.COMMIT;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11575")
	public void testSingleRevisionInTransaction() {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();

		SequenceIdRevisionEntity revisionBeforeFlush = getAuditReader().getCurrentRevision( SequenceIdRevisionEntity.class, true );
		int revisionNumberBeforeFlush = revisionBeforeFlush.getId();

		em.flush();

		StrTestEntity entity = new StrTestEntity( "entity" );
		em.persist( entity );

		em.getTransaction().commit();

		SequenceIdRevisionEntity entity2Revision = (SequenceIdRevisionEntity) ( (Object[]) getAuditReader().createQuery()
				.forRevisionsOfEntity( StrTestEntity.class, false, false ).add( AuditEntity.id().eq( entity.getId() ) ).getSingleResult() )[1];

		assertEquals(
				"The revision number obtained before the flush and the persisting of the entity should be the same as the revision number of the entity because there should only be one revision number per transaction",
				revisionNumberBeforeFlush,
				entity2Revision.getId() );

	}

}
