/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.CollectionRefIngEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicCollection extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CollectionRefEdEntity.class, CollectionRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		CollectionRefEdEntity ed1 = new CollectionRefEdEntity( 1, "data_ed_1" );
		CollectionRefEdEntity ed2 = new CollectionRefEdEntity( 2, "data_ed_2" );

		CollectionRefIngEntity ing1 = new CollectionRefIngEntity( 3, "data_ing_1", ed1 );
		CollectionRefIngEntity ing2 = new CollectionRefIngEntity( 4, "data_ing_2", ed1 );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ing1 = em.find( CollectionRefIngEntity.class, ing1.getId() );
		ed2 = em.find( CollectionRefEdEntity.class, ed2.getId() );

		ing1.setReference( ed2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing2 = em.find( CollectionRefIngEntity.class, ing2.getId() );
		ed2 = em.find( CollectionRefEdEntity.class, ed2.getId() );

		ing2.setReference( ed2 );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( CollectionRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( CollectionRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( CollectionRefIngEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( CollectionRefIngEntity.class, ing2_id ) );
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
		CollectionRefIngEntity ing1 = getEntityManager().find( CollectionRefIngEntity.class, ing1_id );
		CollectionRefIngEntity ing2 = getEntityManager().find( CollectionRefIngEntity.class, ing2_id );

		CollectionRefEdEntity rev1 = getAuditReader().find( CollectionRefEdEntity.class, ed1_id, 1 );
		CollectionRefEdEntity rev2 = getAuditReader().find( CollectionRefEdEntity.class, ed1_id, 2 );
		CollectionRefEdEntity rev3 = getAuditReader().find( CollectionRefEdEntity.class, ed1_id, 3 );

		assert rev1.getReffering().containsAll( makeSet( ing1, ing2 ) );
		assert rev1.getReffering().size() == 2;

		assert rev2.getReffering().containsAll( makeSet( ing2 ) );
		assert rev2.getReffering().size() == 1;

		assert rev3.getReffering().containsAll( Collections.EMPTY_SET );
		assert rev3.getReffering().size() == 0;
	}

	@Test
	public void testHistoryOfEdId2() {
		CollectionRefIngEntity ing1 = getEntityManager().find( CollectionRefIngEntity.class, ing1_id );
		CollectionRefIngEntity ing2 = getEntityManager().find( CollectionRefIngEntity.class, ing2_id );

		CollectionRefEdEntity rev1 = getAuditReader().find( CollectionRefEdEntity.class, ed2_id, 1 );
		CollectionRefEdEntity rev2 = getAuditReader().find( CollectionRefEdEntity.class, ed2_id, 2 );
		CollectionRefEdEntity rev3 = getAuditReader().find( CollectionRefEdEntity.class, ed2_id, 3 );

		assert rev1.getReffering().containsAll( Collections.EMPTY_SET );
		assert rev1.getReffering().size() == 0;

		assert rev2.getReffering().containsAll( makeSet( ing1 ) );
		assert rev2.getReffering().size() == 1;

		assert rev3.getReffering().containsAll( makeSet( ing1, ing2 ) );
		assert rev3.getReffering().size() == 2;

	}

	@Test
	public void testHistoryOfEdIng1() {
		CollectionRefEdEntity ed1 = getEntityManager().find( CollectionRefEdEntity.class, ed1_id );
		CollectionRefEdEntity ed2 = getEntityManager().find( CollectionRefEdEntity.class, ed2_id );

		CollectionRefIngEntity rev1 = getAuditReader().find( CollectionRefIngEntity.class, ing1_id, 1 );
		CollectionRefIngEntity rev2 = getAuditReader().find( CollectionRefIngEntity.class, ing1_id, 2 );
		CollectionRefIngEntity rev3 = getAuditReader().find( CollectionRefIngEntity.class, ing1_id, 3 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed2 );
		assert rev3.getReference().equals( ed2 );
	}

	@Test
	public void testHistoryOfEdIng2() {
		CollectionRefEdEntity ed1 = getEntityManager().find( CollectionRefEdEntity.class, ed1_id );
		CollectionRefEdEntity ed2 = getEntityManager().find( CollectionRefEdEntity.class, ed2_id );

		CollectionRefIngEntity rev1 = getAuditReader().find( CollectionRefIngEntity.class, ing2_id, 1 );
		CollectionRefIngEntity rev2 = getAuditReader().find( CollectionRefIngEntity.class, ing2_id, 2 );
		CollectionRefIngEntity rev3 = getAuditReader().find( CollectionRefIngEntity.class, ing2_id, 3 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed1 );
		assert rev3.getReference().equals( ed2 );
	}
}
