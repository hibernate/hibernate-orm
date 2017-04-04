/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetoone.bidirectional;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerHbmRefEdPK;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerHbmRefIngPK;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Erik-Berndt Scheper, Amar Singh
 */
@TestForIssue(jiraKey = "HHH-3854")
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