/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey( value = "HHH-8243" )
public class CommitFlush extends AbstractFlushTest {
	private Integer id = null;

	@Override
	public FlushMode getFlushMode() {
		return FlushMode.COMMIT;
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "x" );
		em.persist( entity );
		em.getTransaction().commit();

		id = entity.getId();

		// Revision 2
		em.getTransaction().begin();
		entity = em.find( StrTestEntity.class, entity.getId() );
		entity.setStr( "y" );
		entity = em.merge( entity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( StrTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		StrTestEntity ver1 = new StrTestEntity( "x", id );
		StrTestEntity ver2 = new StrTestEntity( "y", id );

		assertEquals( ver1, getAuditReader().find( StrTestEntity.class, id, 1 ) );
		assertEquals( ver2, getAuditReader().find( StrTestEntity.class, id, 2 ) );
	}

	@Test
	public void testCurrent() {
		assertEquals( new StrTestEntity( "y", id ), getEntityManager().find( StrTestEntity.class, id ) );
	}

	@Test
	public void testRevisionTypes() {
		List<Object[]> results = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id ) )
				.getResultList();

		assertEquals( results.get( 0 )[2], RevisionType.ADD );
		assertEquals( results.get( 1 )[2], RevisionType.MOD );
	}
}
