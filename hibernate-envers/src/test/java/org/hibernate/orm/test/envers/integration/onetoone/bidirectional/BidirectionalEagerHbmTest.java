/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.onetoone.BidirectionalEagerHbmRefEdPK;
import org.hibernate.orm.test.envers.entities.onetoone.BidirectionalEagerHbmRefIngPK;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Erik-Berndt Scheper, Amar Singh
 */
@JiraKey(value = "HHH-3854")
public class BidirectionalEagerHbmTest extends BaseEnversJPAFunctionalTestCase {
	private Long refIngId1 = null;

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/oneToOne/bidirectional/eagerLoading.hbm.xml"};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		BidirectionalEagerHbmRefEdPK ed1 = new BidirectionalEagerHbmRefEdPK( "data_ed_1" );
		BidirectionalEagerHbmRefIngPK ing1 = new BidirectionalEagerHbmRefIngPK( "data_ing_1" );
		ing1.setReference( ed1 );
		em.persist( ed1 );
		em.persist( ing1 );
		em.getTransaction().commit();

		refIngId1 = ing1.getId();

		em.close();
	}

	@Test
	public void testNonProxyObjectTraversing() {
		BidirectionalEagerHbmRefIngPK referencing =
				getAuditReader().find( BidirectionalEagerHbmRefIngPK.class, refIngId1, 1 );
		assertNotNull( referencing.getReference().getData() );
	}
}
