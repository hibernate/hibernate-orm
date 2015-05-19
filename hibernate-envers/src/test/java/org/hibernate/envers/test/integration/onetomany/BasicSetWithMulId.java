/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.MulId;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefEdMulIdEntity;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefIngMulIdEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSetWithMulId extends BaseEnversJPAFunctionalTestCase {
	private MulId ed1_id;
	private MulId ed2_id;

	private MulId ing1_id;
	private MulId ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SetRefEdMulIdEntity.class, SetRefIngMulIdEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		ed1_id = new MulId( 0, 1 );
		ed2_id = new MulId( 2, 3 );

		ing2_id = new MulId( 4, 5 );
		ing1_id = new MulId( 6, 7 );

		EntityManager em = getEntityManager();

		SetRefEdMulIdEntity ed1 = new SetRefEdMulIdEntity( ed1_id.getId1(), ed1_id.getId2(), "data_ed_1" );
		SetRefEdMulIdEntity ed2 = new SetRefEdMulIdEntity( ed2_id.getId1(), ed2_id.getId2(), "data_ed_2" );

		SetRefIngMulIdEntity ing1 = new SetRefIngMulIdEntity( ing1_id.getId1(), ing1_id.getId2(), "data_ing_1", ed1 );
		SetRefIngMulIdEntity ing2 = new SetRefIngMulIdEntity( ing2_id.getId1(), ing2_id.getId2(), "data_ing_2", ed1 );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ing1 = em.find( SetRefIngMulIdEntity.class, ing1_id );
		ed2 = em.find( SetRefEdMulIdEntity.class, ed2_id );

		ing1.setReference( ed2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing2 = em.find( SetRefIngMulIdEntity.class, ing2_id );
		ed2 = em.find( SetRefEdMulIdEntity.class, ed2_id );

		ing2.setReference( ed2 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( SetRefEdMulIdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( SetRefEdMulIdEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SetRefIngMulIdEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( SetRefIngMulIdEntity.class, ing2_id ) );
	}

	private <T> Set<T> makeSet(T... objects) {
		Set<T> ret = new HashSet<T>();
		//noinspection ManualArrayToCollectionCopy
		for ( T obj : objects ) {
			ret.add( obj );
		}
		return ret;
	}

	@Test
	public void testHistoryOfEdId1() {
		SetRefIngMulIdEntity ing1 = getEntityManager().find( SetRefIngMulIdEntity.class, ing1_id );
		SetRefIngMulIdEntity ing2 = getEntityManager().find( SetRefIngMulIdEntity.class, ing2_id );

		SetRefEdMulIdEntity rev1 = getAuditReader().find( SetRefEdMulIdEntity.class, ed1_id, 1 );
		SetRefEdMulIdEntity rev2 = getAuditReader().find( SetRefEdMulIdEntity.class, ed1_id, 2 );
		SetRefEdMulIdEntity rev3 = getAuditReader().find( SetRefEdMulIdEntity.class, ed1_id, 3 );

		assert rev1.getReffering().equals( makeSet( ing1, ing2 ) );
		assert rev2.getReffering().equals( makeSet( ing2 ) );
		assert rev3.getReffering().equals( Collections.EMPTY_SET );
	}

	@Test
	public void testHistoryOfEdId2() {
		SetRefIngMulIdEntity ing1 = getEntityManager().find( SetRefIngMulIdEntity.class, ing1_id );
		SetRefIngMulIdEntity ing2 = getEntityManager().find( SetRefIngMulIdEntity.class, ing2_id );

		SetRefEdMulIdEntity rev1 = getAuditReader().find( SetRefEdMulIdEntity.class, ed2_id, 1 );
		SetRefEdMulIdEntity rev2 = getAuditReader().find( SetRefEdMulIdEntity.class, ed2_id, 2 );
		SetRefEdMulIdEntity rev3 = getAuditReader().find( SetRefEdMulIdEntity.class, ed2_id, 3 );

		assert rev1.getReffering().equals( Collections.EMPTY_SET );
		assert rev2.getReffering().equals( makeSet( ing1 ) );
		assert rev3.getReffering().equals( makeSet( ing1, ing2 ) );
	}

	@Test
	public void testHistoryOfEdIng1() {
		SetRefEdMulIdEntity ed1 = getEntityManager().find( SetRefEdMulIdEntity.class, ed1_id );
		SetRefEdMulIdEntity ed2 = getEntityManager().find( SetRefEdMulIdEntity.class, ed2_id );

		SetRefIngMulIdEntity rev1 = getAuditReader().find( SetRefIngMulIdEntity.class, ing1_id, 1 );
		SetRefIngMulIdEntity rev2 = getAuditReader().find( SetRefIngMulIdEntity.class, ing1_id, 2 );
		SetRefIngMulIdEntity rev3 = getAuditReader().find( SetRefIngMulIdEntity.class, ing1_id, 3 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed2 );
		assert rev3.getReference().equals( ed2 );
	}

	@Test
	public void testHistoryOfEdIng2() {
		SetRefEdMulIdEntity ed1 = getEntityManager().find( SetRefEdMulIdEntity.class, ed1_id );
		SetRefEdMulIdEntity ed2 = getEntityManager().find( SetRefEdMulIdEntity.class, ed2_id );

		SetRefIngMulIdEntity rev1 = getAuditReader().find( SetRefIngMulIdEntity.class, ing2_id, 1 );
		SetRefIngMulIdEntity rev2 = getAuditReader().find( SetRefIngMulIdEntity.class, ing2_id, 2 );
		SetRefIngMulIdEntity rev3 = getAuditReader().find( SetRefIngMulIdEntity.class, ing2_id, 3 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed1 );
		assert rev3.getReference().equals( ed2 );
	}
}