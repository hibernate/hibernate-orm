/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.onetoone.bidirectional;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerHbmRefEdPK;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerHbmRefIngPK;

import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertNotNull;

/**
 * @author Erik-Berndt Scheper, Amar Singh
 */
@TestForIssue(jiraKey = "HHH-3854")
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
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