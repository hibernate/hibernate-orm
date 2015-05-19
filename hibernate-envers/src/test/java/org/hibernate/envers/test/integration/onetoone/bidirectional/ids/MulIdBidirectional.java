/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetoone.bidirectional.ids;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.MulId;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MulIdBidirectional extends BaseEnversJPAFunctionalTestCase {
	private MulId ed1_id;
	private MulId ed2_id;

	private MulId ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BiMulIdRefEdEntity.class, BiMulIdRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		ed1_id = new MulId( 1, 2 );
		ed2_id = new MulId( 3, 4 );

		ing1_id = new MulId( 5, 6 );

		BiMulIdRefEdEntity ed1 = new BiMulIdRefEdEntity( ed1_id.getId1(), ed1_id.getId2(), "data_ed_1" );
		BiMulIdRefEdEntity ed2 = new BiMulIdRefEdEntity( ed2_id.getId1(), ed2_id.getId2(), "data_ed_2" );

		BiMulIdRefIngEntity ing1 = new BiMulIdRefIngEntity( ing1_id.getId1(), ing1_id.getId2(), "data_ing_1" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		ing1.setReference( ed1 );

		em.persist( ed1 );
		em.persist( ed2 );

		em.persist( ing1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ing1 = em.find( BiMulIdRefIngEntity.class, ing1_id );
		ed2 = em.find( BiMulIdRefEdEntity.class, ed2_id );

		ing1.setReference( ed2 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiMulIdRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiMulIdRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiMulIdRefIngEntity.class, ing1_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		BiMulIdRefIngEntity ing1 = getEntityManager().find( BiMulIdRefIngEntity.class, ing1_id );

		BiMulIdRefEdEntity rev1 = getAuditReader().find( BiMulIdRefEdEntity.class, ed1_id, 1 );
		BiMulIdRefEdEntity rev2 = getAuditReader().find( BiMulIdRefEdEntity.class, ed1_id, 2 );

		assert rev1.getReferencing().equals( ing1 );
		assert rev2.getReferencing() == null;
	}

	@Test
	public void testHistoryOfEdId2() {
		BiMulIdRefIngEntity ing1 = getEntityManager().find( BiMulIdRefIngEntity.class, ing1_id );

		BiMulIdRefEdEntity rev1 = getAuditReader().find( BiMulIdRefEdEntity.class, ed2_id, 1 );
		BiMulIdRefEdEntity rev2 = getAuditReader().find( BiMulIdRefEdEntity.class, ed2_id, 2 );

		assert rev1.getReferencing() == null;
		assert rev2.getReferencing().equals( ing1 );
	}

	@Test
	public void testHistoryOfIngId1() {
		BiMulIdRefEdEntity ed1 = getEntityManager().find( BiMulIdRefEdEntity.class, ed1_id );
		BiMulIdRefEdEntity ed2 = getEntityManager().find( BiMulIdRefEdEntity.class, ed2_id );

		BiMulIdRefIngEntity rev1 = getAuditReader().find( BiMulIdRefIngEntity.class, ing1_id, 1 );
		BiMulIdRefIngEntity rev2 = getAuditReader().find( BiMulIdRefIngEntity.class, ing1_id, 2 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed2 );
	}
}