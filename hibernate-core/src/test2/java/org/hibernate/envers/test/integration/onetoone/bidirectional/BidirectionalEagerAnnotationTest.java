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
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerAnnotationRefEdOneToOne;
import org.hibernate.envers.test.entities.onetoone.BidirectionalEagerAnnotationRefIngOneToOne;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Erik-Berndt Scheper
 */
@TestForIssue(jiraKey = "HHH-3854")
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