/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import org.hibernate.FlushMode;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = StrTestEntity.class)
@SessionFactory
public class CommitFlushSingleRevisionInTransaction {

	@Test
	@JiraKey(value = "HHH-11575")
	public void testSingleRevisionInTransaction(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.setHibernateFlushMode( FlushMode.COMMIT );
			session.getTransaction().begin();

			final var auditReader = AuditReaderFactory.get( session );
			SequenceIdRevisionEntity revisionBeforeFlush = auditReader.getCurrentRevision(
					SequenceIdRevisionEntity.class, true );
			int revisionNumberBeforeFlush = revisionBeforeFlush.getId();

			session.flush();

			StrTestEntity entity = new StrTestEntity( "entity" );
			session.persist( entity );

			session.getTransaction().commit();

			SequenceIdRevisionEntity entity2Revision = (SequenceIdRevisionEntity) ((Object[]) auditReader.createQuery()
					.forRevisionsOfEntity( StrTestEntity.class, false, false )
					.add( AuditEntity.id().eq( entity.getId() ) )
					.getSingleResult())[1];


			assertEquals(
					revisionNumberBeforeFlush,
					entity2Revision.getId(),
					"The revision number obtained before the flush and the persisting of the entity should be the same as the revision number of the entity because there should only be one revision number per transaction"
			);
		} );
	}
}
