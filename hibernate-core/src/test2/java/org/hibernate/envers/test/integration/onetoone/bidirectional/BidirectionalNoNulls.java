/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetoone.bidirectional;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BidirectionalNoNulls extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BiRefEdEntity.class, BiRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
		BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

		BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );
		BiRefIngEntity ing2 = new BiRefIngEntity( 4, "data_ing_2" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		ing1.setReference( ed1 );
		ing2.setReference( ed2 );

		em.persist( ed1 );
		em.persist( ed2 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		ing1 = em.find( BiRefIngEntity.class, ing1.getId() );
		ing2 = em.find( BiRefIngEntity.class, ing2.getId() );

		ed1 = em.find( BiRefEdEntity.class, ed1.getId() );
		ed2 = em.find( BiRefEdEntity.class, ed2.getId() );

		ing1.setReference( ed2 );
		ing2.setReference( ed1 );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiRefIngEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiRefIngEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		BiRefIngEntity ing1 = getEntityManager().find( BiRefIngEntity.class, ing1_id );
		BiRefIngEntity ing2 = getEntityManager().find( BiRefIngEntity.class, ing2_id );

		BiRefEdEntity rev1 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 1 );
		BiRefEdEntity rev2 = getAuditReader().find( BiRefEdEntity.class, ed1_id, 2 );

		assert rev1.getReferencing().equals( ing1 );
		assert rev2.getReferencing().equals( ing2 );
	}

	@Test
	public void testHistoryOfEdId2() {
		BiRefIngEntity ing1 = getEntityManager().find( BiRefIngEntity.class, ing1_id );
		BiRefIngEntity ing2 = getEntityManager().find( BiRefIngEntity.class, ing2_id );

		BiRefEdEntity rev1 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 1 );
		BiRefEdEntity rev2 = getAuditReader().find( BiRefEdEntity.class, ed2_id, 2 );

		assert rev1.getReferencing().equals( ing2 );
		assert rev2.getReferencing().equals( ing1 );
	}
}
