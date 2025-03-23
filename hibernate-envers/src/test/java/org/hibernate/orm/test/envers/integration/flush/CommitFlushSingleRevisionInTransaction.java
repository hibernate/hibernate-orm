/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.orm.junit.JiraKey;
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
	@JiraKey(value = "HHH-11575")
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
