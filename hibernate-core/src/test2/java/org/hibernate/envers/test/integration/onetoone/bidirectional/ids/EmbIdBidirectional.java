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
import org.hibernate.envers.test.entities.ids.EmbId;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EmbIdBidirectional extends BaseEnversJPAFunctionalTestCase {
	private EmbId ed1_id;
	private EmbId ed2_id;

	private EmbId ing1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BiEmbIdRefEdEntity.class, BiEmbIdRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		ed1_id = new EmbId( 1, 2 );
		ed2_id = new EmbId( 3, 4 );

		ing1_id = new EmbId( 5, 6 );

		BiEmbIdRefEdEntity ed1 = new BiEmbIdRefEdEntity( ed1_id, "data_ed_1" );
		BiEmbIdRefEdEntity ed2 = new BiEmbIdRefEdEntity( ed2_id, "data_ed_2" );

		BiEmbIdRefIngEntity ing1 = new BiEmbIdRefIngEntity( ing1_id, "data_ing_1" );

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

		ing1 = em.find( BiEmbIdRefIngEntity.class, ing1.getId() );
		ed2 = em.find( BiEmbIdRefEdEntity.class, ed2.getId() );

		ing1.setReference( ed2 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiEmbIdRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiEmbIdRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( BiEmbIdRefIngEntity.class, ing1_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		BiEmbIdRefIngEntity ing1 = getEntityManager().find( BiEmbIdRefIngEntity.class, ing1_id );

		BiEmbIdRefEdEntity rev1 = getAuditReader().find( BiEmbIdRefEdEntity.class, ed1_id, 1 );
		BiEmbIdRefEdEntity rev2 = getAuditReader().find( BiEmbIdRefEdEntity.class, ed1_id, 2 );

		assert rev1.getReferencing().equals( ing1 );
		assert rev2.getReferencing() == null;
	}

	@Test
	public void testHistoryOfEdId2() {
		BiEmbIdRefIngEntity ing1 = getEntityManager().find( BiEmbIdRefIngEntity.class, ing1_id );

		BiEmbIdRefEdEntity rev1 = getAuditReader().find( BiEmbIdRefEdEntity.class, ed2_id, 1 );
		BiEmbIdRefEdEntity rev2 = getAuditReader().find( BiEmbIdRefEdEntity.class, ed2_id, 2 );

		assert rev1.getReferencing() == null;
		assert rev2.getReferencing().equals( ing1 );
	}

	@Test
	public void testHistoryOfIngId1() {
		BiEmbIdRefEdEntity ed1 = getEntityManager().find( BiEmbIdRefEdEntity.class, ed1_id );
		BiEmbIdRefEdEntity ed2 = getEntityManager().find( BiEmbIdRefEdEntity.class, ed2_id );

		BiEmbIdRefIngEntity rev1 = getAuditReader().find( BiEmbIdRefIngEntity.class, ing1_id, 1 );
		BiEmbIdRefIngEntity rev2 = getAuditReader().find( BiEmbIdRefIngEntity.class, ing1_id, 2 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed2 );
	}
}