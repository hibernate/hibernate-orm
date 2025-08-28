/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.onetomany.ListRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ListRefIngEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicList extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ListRefEdEntity.class, ListRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		ListRefEdEntity ed1 = new ListRefEdEntity( 1, "data_ed_1" );
		ListRefEdEntity ed2 = new ListRefEdEntity( 2, "data_ed_2" );

		ListRefIngEntity ing1 = new ListRefIngEntity( 3, "data_ing_1", ed1 );
		ListRefIngEntity ing2 = new ListRefIngEntity( 4, "data_ing_2", ed1 );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ing1 = em.find( ListRefIngEntity.class, ing1.getId() );
		ed2 = em.find( ListRefEdEntity.class, ed2.getId() );

		ing1.setReference( ed2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing2 = em.find( ListRefIngEntity.class, ing2.getId() );
		ed2 = em.find( ListRefEdEntity.class, ed2.getId() );

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
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( ListRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( ListRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ListRefIngEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( ListRefIngEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		ListRefIngEntity ing1 = getEntityManager().find( ListRefIngEntity.class, ing1_id );
		ListRefIngEntity ing2 = getEntityManager().find( ListRefIngEntity.class, ing2_id );

		ListRefEdEntity rev1 = getAuditReader().find( ListRefEdEntity.class, ed1_id, 1 );
		ListRefEdEntity rev2 = getAuditReader().find( ListRefEdEntity.class, ed1_id, 2 );
		ListRefEdEntity rev3 = getAuditReader().find( ListRefEdEntity.class, ed1_id, 3 );

		assert TestTools.checkCollection( rev1.getReffering(), ing1, ing2 );
		assert TestTools.checkCollection( rev2.getReffering(), ing2 );
		assert TestTools.checkCollection( rev3.getReffering() );
	}

	@Test
	public void testHistoryOfEdId2() {
		ListRefIngEntity ing1 = getEntityManager().find( ListRefIngEntity.class, ing1_id );
		ListRefIngEntity ing2 = getEntityManager().find( ListRefIngEntity.class, ing2_id );

		ListRefEdEntity rev1 = getAuditReader().find( ListRefEdEntity.class, ed2_id, 1 );
		ListRefEdEntity rev2 = getAuditReader().find( ListRefEdEntity.class, ed2_id, 2 );
		ListRefEdEntity rev3 = getAuditReader().find( ListRefEdEntity.class, ed2_id, 3 );

		assert TestTools.checkCollection( rev1.getReffering() );
		assert TestTools.checkCollection( rev2.getReffering(), ing1 );
		assert TestTools.checkCollection( rev3.getReffering(), ing1, ing2 );
	}

	@Test
	public void testHistoryOfEdIng1() {
		ListRefEdEntity ed1 = getEntityManager().find( ListRefEdEntity.class, ed1_id );
		ListRefEdEntity ed2 = getEntityManager().find( ListRefEdEntity.class, ed2_id );

		ListRefIngEntity rev1 = getAuditReader().find( ListRefIngEntity.class, ing1_id, 1 );
		ListRefIngEntity rev2 = getAuditReader().find( ListRefIngEntity.class, ing1_id, 2 );
		ListRefIngEntity rev3 = getAuditReader().find( ListRefIngEntity.class, ing1_id, 3 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed2 );
		assert rev3.getReference().equals( ed2 );
	}

	@Test
	public void testHistoryOfEdIng2() {
		ListRefEdEntity ed1 = getEntityManager().find( ListRefEdEntity.class, ed1_id );
		ListRefEdEntity ed2 = getEntityManager().find( ListRefEdEntity.class, ed2_id );

		ListRefIngEntity rev1 = getAuditReader().find( ListRefIngEntity.class, ing2_id, 1 );
		ListRefIngEntity rev2 = getAuditReader().find( ListRefIngEntity.class, ing2_id, 2 );
		ListRefIngEntity rev3 = getAuditReader().find( ListRefIngEntity.class, ing2_id, 3 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed1 );
		assert rev3.getReference().equals( ed2 );
	}
}
