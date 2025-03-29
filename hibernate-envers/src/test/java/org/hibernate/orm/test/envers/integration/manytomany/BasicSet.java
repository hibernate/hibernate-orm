/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.manytomany.SetOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.SetOwningEntity;
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
		return new Class[] {SetOwningEntity.class, SetOwnedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		SetOwnedEntity ed1 = new SetOwnedEntity( 1, "data_ed_1" );
		SetOwnedEntity ed2 = new SetOwnedEntity( 2, "data_ed_2" );

		SetOwningEntity ing1 = new SetOwningEntity( 3, "data_ing_1" );
		SetOwningEntity ing2 = new SetOwningEntity( 4, "data_ing_2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2

		em.getTransaction().begin();

		ing1 = em.find( SetOwningEntity.class, ing1.getId() );
		ing2 = em.find( SetOwningEntity.class, ing2.getId() );
		ed1 = em.find( SetOwnedEntity.class, ed1.getId() );
		ed2 = em.find( SetOwnedEntity.class, ed2.getId() );

		ing1.setReferences( new HashSet<SetOwnedEntity>() );
		ing1.getReferences().add( ed1 );

		ing2.setReferences( new HashSet<SetOwnedEntity>() );
		ing2.getReferences().add( ed1 );
		ing2.getReferences().add( ed2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing1 = em.find( SetOwningEntity.class, ing1.getId() );
		ed2 = em.find( SetOwnedEntity.class, ed2.getId() );
		ed1 = em.find( SetOwnedEntity.class, ed1.getId() );

		ing1.getReferences().add( ed2 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		ing1 = em.find( SetOwningEntity.class, ing1.getId() );
		ed2 = em.find( SetOwnedEntity.class, ed2.getId() );
		ed1 = em.find( SetOwnedEntity.class, ed1.getId() );

		ing1.getReferences().remove( ed1 );

		em.getTransaction().commit();

		// Revision 5
		em.getTransaction().begin();

		ing1 = em.find( SetOwningEntity.class, ing1.getId() );

		ing1.setReferences( null );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 4 ).equals( getAuditReader().getRevisions( SetOwnedEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2, 3, 5 ).equals( getAuditReader().getRevisions( SetOwnedEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2, 3, 4, 5 ).equals( getAuditReader().getRevisions( SetOwningEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SetOwningEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		SetOwningEntity ing1 = getEntityManager().find( SetOwningEntity.class, ing1_id );
		SetOwningEntity ing2 = getEntityManager().find( SetOwningEntity.class, ing2_id );

		SetOwnedEntity rev1 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 1 );
		SetOwnedEntity rev2 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 2 );
		SetOwnedEntity rev3 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 3 );
		SetOwnedEntity rev4 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 4 );
		SetOwnedEntity rev5 = getAuditReader().find( SetOwnedEntity.class, ed1_id, 5 );

		assert rev1.getReferencing().equals( Collections.EMPTY_SET );
		assert rev2.getReferencing().equals( TestTools.makeSet( ing1, ing2 ) );
		assert rev3.getReferencing().equals( TestTools.makeSet( ing1, ing2 ) );
		assert rev4.getReferencing().equals( TestTools.makeSet( ing2 ) );
		assert rev5.getReferencing().equals( TestTools.makeSet( ing2 ) );
	}

	@Test
	public void testHistoryOfEdId2() {
		SetOwningEntity ing1 = getEntityManager().find( SetOwningEntity.class, ing1_id );
		SetOwningEntity ing2 = getEntityManager().find( SetOwningEntity.class, ing2_id );

		SetOwnedEntity rev1 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 1 );
		SetOwnedEntity rev2 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 2 );
		SetOwnedEntity rev3 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 3 );
		SetOwnedEntity rev4 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 4 );
		SetOwnedEntity rev5 = getAuditReader().find( SetOwnedEntity.class, ed2_id, 5 );

		assert rev1.getReferencing().equals( Collections.EMPTY_SET );
		assert rev2.getReferencing().equals( TestTools.makeSet( ing2 ) );
		assert rev3.getReferencing().equals( TestTools.makeSet( ing1, ing2 ) );
		assert rev4.getReferencing().equals( TestTools.makeSet( ing1, ing2 ) );
		assert rev5.getReferencing().equals( TestTools.makeSet( ing2 ) );
	}

	@Test
	public void testHistoryOfEdIng1() {
		SetOwnedEntity ed1 = getEntityManager().find( SetOwnedEntity.class, ed1_id );
		SetOwnedEntity ed2 = getEntityManager().find( SetOwnedEntity.class, ed2_id );

		SetOwningEntity rev1 = getAuditReader().find( SetOwningEntity.class, ing1_id, 1 );
		SetOwningEntity rev2 = getAuditReader().find( SetOwningEntity.class, ing1_id, 2 );
		SetOwningEntity rev3 = getAuditReader().find( SetOwningEntity.class, ing1_id, 3 );
		SetOwningEntity rev4 = getAuditReader().find( SetOwningEntity.class, ing1_id, 4 );
		SetOwningEntity rev5 = getAuditReader().find( SetOwningEntity.class, ing1_id, 5 );

		assert rev1.getReferences().equals( Collections.EMPTY_SET );
		assert rev2.getReferences().equals( TestTools.makeSet( ed1 ) );
		assert rev3.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
		assert rev4.getReferences().equals( TestTools.makeSet( ed2 ) );
		assert rev5.getReferences().equals( Collections.EMPTY_SET );
	}

	@Test
	public void testHistoryOfEdIng2() {
		SetOwnedEntity ed1 = getEntityManager().find( SetOwnedEntity.class, ed1_id );
		SetOwnedEntity ed2 = getEntityManager().find( SetOwnedEntity.class, ed2_id );

		SetOwningEntity rev1 = getAuditReader().find( SetOwningEntity.class, ing2_id, 1 );
		SetOwningEntity rev2 = getAuditReader().find( SetOwningEntity.class, ing2_id, 2 );
		SetOwningEntity rev3 = getAuditReader().find( SetOwningEntity.class, ing2_id, 3 );
		SetOwningEntity rev4 = getAuditReader().find( SetOwningEntity.class, ing2_id, 4 );
		SetOwningEntity rev5 = getAuditReader().find( SetOwningEntity.class, ing2_id, 5 );

		assert rev1.getReferences().equals( Collections.EMPTY_SET );
		assert rev2.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
		assert rev3.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
		assert rev4.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
		assert rev5.getReferences().equals( TestTools.makeSet( ed1, ed2 ) );
	}
}
