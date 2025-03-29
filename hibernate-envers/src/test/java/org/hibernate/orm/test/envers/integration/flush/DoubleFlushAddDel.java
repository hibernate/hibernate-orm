/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DoubleFlushAddDel extends AbstractFlushTest {
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

		em.remove( em.find( StrTestEntity.class, fe.getId() ) );

		em.flush();

		em.getTransaction().commit();

		//

		id = fe.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList().equals( getAuditReader().getRevisions( StrTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		assert getAuditReader().find( StrTestEntity.class, id, 1 ) == null;
	}
}
