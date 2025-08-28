/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids;

import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.ids.ManyToOneIdNotAuditedTestEntity;
import org.hibernate.orm.test.envers.entities.ids.ManyToOneNotAuditedEmbId;

import org.junit.Test;

/**
 * A test checking that when using Envers it is possible to have non-audited entities that use unsupported
 * components in their ids, e.g. a many-to-one join to another entity.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class ManyToOneIdNotAudited extends BaseEnversJPAFunctionalTestCase {
	private ManyToOneNotAuditedEmbId id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ManyToOneIdNotAuditedTestEntity.class, UnversionedStrTestEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		UnversionedStrTestEntity uste = new UnversionedStrTestEntity();
		uste.setStr( "test1" );
		em.persist( uste );

		id1 = new ManyToOneNotAuditedEmbId( uste );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		ManyToOneIdNotAuditedTestEntity mtoinate = new ManyToOneIdNotAuditedTestEntity();
		mtoinate.setData( "data1" );
		mtoinate.setId( id1 );
		em.persist( mtoinate );

		em.getTransaction().commit();
	}
}
