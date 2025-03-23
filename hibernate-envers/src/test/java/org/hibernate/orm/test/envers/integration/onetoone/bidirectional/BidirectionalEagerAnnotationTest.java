/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.onetoone.BidirectionalEagerAnnotationRefEdOneToOne;
import org.hibernate.orm.test.envers.entities.onetoone.BidirectionalEagerAnnotationRefIngOneToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Erik-Berndt Scheper
 */
@JiraKey(value = "HHH-3854")
public class BidirectionalEagerAnnotationTest extends BaseEnversJPAFunctionalTestCase {
	private Integer refIngId1 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				BidirectionalEagerAnnotationRefEdOneToOne.class,
				BidirectionalEagerAnnotationRefIngOneToOne.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		BidirectionalEagerAnnotationRefEdOneToOne ed1 = new BidirectionalEagerAnnotationRefEdOneToOne();
		BidirectionalEagerAnnotationRefIngOneToOne ing1 = new BidirectionalEagerAnnotationRefIngOneToOne();
		ed1.setData( "referredEntity1" );
		ed1.setRefIng( ing1 );
		ing1.setData( "referringEntity" );
		ing1.setRefedOne( ed1 );
		em.persist( ed1 );
		em.persist( ing1 );
		em.getTransaction().commit();

		refIngId1 = ing1.getId();

		em.close();
	}

	@Test
	public void testNonProxyObjectTraversing() {
		BidirectionalEagerAnnotationRefIngOneToOne referencing =
				getAuditReader().find( BidirectionalEagerAnnotationRefIngOneToOne.class, refIngId1, 1 );
		assertNotNull( referencing.getRefedOne().getData() );
	}
}
