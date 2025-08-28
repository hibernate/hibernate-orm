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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DoubleFlushModDel extends AbstractFlushTest {
	private Integer id;

	public FlushMode getFlushMode() {
		return FlushMode.MANUAL;
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrTestEntity fe = new StrTestEntity( "x" );
		em.persist( fe );
		em.flush();

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		fe = em.find( StrTestEntity.class, fe.getId() );

		fe.setStr( "y" );
		em.flush();

		em.remove( em.find( StrTestEntity.class, fe.getId() ) );
		em.flush();

		em.getTransaction().commit();

		//

		id = fe.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( StrTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		StrTestEntity ver1 = new StrTestEntity( "x", id );

		assert getAuditReader().find( StrTestEntity.class, id, 1 ).equals( ver1 );
		assert getAuditReader().find( StrTestEntity.class, id, 2 ) == null;
	}

	@Test
	public void testRevisionTypes() {
		@SuppressWarnings("unchecked") List<Object[]> results =
				getAuditReader().createQuery()
						.forRevisionsOfEntity( StrTestEntity.class, false, true )
						.add( AuditEntity.id().eq( id ) )
						.getResultList();

		assertEquals( results.get( 0 )[2], RevisionType.ADD );
		assertEquals( results.get( 1 )[2], RevisionType.DEL );
	}
}
