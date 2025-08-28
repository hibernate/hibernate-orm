/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSet extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SetRefEdEntity.class, SetRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
		SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

		SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );
		SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		em.getTransaction().commit();

		// Revision 2

		em.getTransaction().begin();

		ed1 = em.find( SetRefEdEntity.class, ed1.getId() );

		ing1.setReference( ed1 );
		ing2.setReference( ed1 );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing1 = em.find( SetRefIngEntity.class, ing1.getId() );
		ed2 = em.find( SetRefEdEntity.class, ed2.getId() );

		ing1.setReference( ed2 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		ing2 = em.find( SetRefIngEntity.class, ing2.getId() );
		ed2 = em.find( SetRefEdEntity.class, ed2.getId() );

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
		assert Arrays.asList( 1, 2, 3, 4 ).equals( getAuditReader().getRevisions( SetRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 3, 4 ).equals( getAuditReader().getRevisions( SetRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 2, 3 ).equals( getAuditReader().getRevisions( SetRefIngEntity.class, ing1_id ) );
		assert Arrays.asList( 2, 4 ).equals( getAuditReader().getRevisions( SetRefIngEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		SetRefIngEntity ing1 = getEntityManager().find( SetRefIngEntity.class, ing1_id );
		SetRefIngEntity ing2 = getEntityManager().find( SetRefIngEntity.class, ing2_id );

		SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );
		SetRefEdEntity rev2 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 2 );
		SetRefEdEntity rev3 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 3 );
		SetRefEdEntity rev4 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 4 );

		assert rev1.getReffering().equals( Collections.EMPTY_SET );
		assert rev2.getReffering().equals( TestTools.makeSet( ing1, ing2 ) );
		assert rev3.getReffering().equals( TestTools.makeSet( ing2 ) );
		assert rev4.getReffering().equals( Collections.EMPTY_SET );
	}

	@Test
	public void testHistoryOfEdId2() {
		SetRefIngEntity ing1 = getEntityManager().find( SetRefIngEntity.class, ing1_id );
		SetRefIngEntity ing2 = getEntityManager().find( SetRefIngEntity.class, ing2_id );

		SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 1 );
		SetRefEdEntity rev2 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 2 );
		SetRefEdEntity rev3 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 3 );
		SetRefEdEntity rev4 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 4 );

		assert rev1.getReffering().equals( Collections.EMPTY_SET );
		assert rev2.getReffering().equals( Collections.EMPTY_SET );
		assert rev3.getReffering().equals( TestTools.makeSet( ing1 ) );
		assert rev4.getReffering().equals( TestTools.makeSet( ing1, ing2 ) );
	}

	@Test
	public void testHistoryOfEdIng1() {
		SetRefEdEntity ed1 = getEntityManager().find( SetRefEdEntity.class, ed1_id );
		SetRefEdEntity ed2 = getEntityManager().find( SetRefEdEntity.class, ed2_id );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 4 );

		assert rev1 == null;
		assert rev2.getReference().equals( ed1 );
		assert rev3.getReference().equals( ed2 );
		assert rev4.getReference().equals( ed2 );
	}

	@Test
	public void testHistoryOfEdIng2() {
		SetRefEdEntity ed1 = getEntityManager().find( SetRefEdEntity.class, ed1_id );
		SetRefEdEntity ed2 = getEntityManager().find( SetRefEdEntity.class, ed2_id );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 4 );

		assert rev1 == null;
		assert rev2.getReference().equals( ed1 );
		assert rev3.getReference().equals( ed1 );
		assert rev4.getReference().equals( ed2 );
	}
}
