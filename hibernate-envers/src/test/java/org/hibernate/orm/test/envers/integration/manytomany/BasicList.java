/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwningEntity;
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
		return new Class[] {ListOwningEntity.class, ListOwnedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		ListOwnedEntity ed1 = new ListOwnedEntity( 1, "data_ed_1" );
		ListOwnedEntity ed2 = new ListOwnedEntity( 2, "data_ed_2" );

		ListOwningEntity ing1 = new ListOwningEntity( 3, "data_ing_1" );
		ListOwningEntity ing2 = new ListOwningEntity( 4, "data_ing_2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2

		em.getTransaction().begin();

		ing1 = em.find( ListOwningEntity.class, ing1.getId() );
		ing2 = em.find( ListOwningEntity.class, ing2.getId() );
		ed1 = em.find( ListOwnedEntity.class, ed1.getId() );
		ed2 = em.find( ListOwnedEntity.class, ed2.getId() );

		ing1.setReferences( new ArrayList<ListOwnedEntity>() );
		ing1.getReferences().add( ed1 );

		ing2.setReferences( new ArrayList<ListOwnedEntity>() );
		ing2.getReferences().add( ed1 );
		ing2.getReferences().add( ed2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing1 = em.find( ListOwningEntity.class, ing1.getId() );
		ed2 = em.find( ListOwnedEntity.class, ed2.getId() );
		ed1 = em.find( ListOwnedEntity.class, ed1.getId() );

		ing1.getReferences().add( ed2 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		ing1 = em.find( ListOwningEntity.class, ing1.getId() );
		ed2 = em.find( ListOwnedEntity.class, ed2.getId() );
		ed1 = em.find( ListOwnedEntity.class, ed1.getId() );

		ing1.getReferences().remove( ed1 );

		em.getTransaction().commit();

		// Revision 5
		em.getTransaction().begin();

		ing1 = em.find( ListOwningEntity.class, ing1.getId() );

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
		assert Arrays.asList( 1, 2, 4 ).equals( getAuditReader().getRevisions( ListOwnedEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2, 3, 5 ).equals( getAuditReader().getRevisions( ListOwnedEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2, 3, 4, 5 ).equals(
				getAuditReader().getRevisions(
						ListOwningEntity.class,
						ing1_id
				)
		);
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ListOwningEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		ListOwningEntity ing1 = getEntityManager().find( ListOwningEntity.class, ing1_id );
		ListOwningEntity ing2 = getEntityManager().find( ListOwningEntity.class, ing2_id );

		ListOwnedEntity rev1 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 1 );
		ListOwnedEntity rev2 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 2 );
		ListOwnedEntity rev3 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 3 );
		ListOwnedEntity rev4 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 4 );
		ListOwnedEntity rev5 = getAuditReader().find( ListOwnedEntity.class, ed1_id, 5 );

		assert rev1.getReferencing().equals( Collections.EMPTY_LIST );
		assert TestTools.checkCollection( rev2.getReferencing(), ing1, ing2 );
		assert TestTools.checkCollection( rev3.getReferencing(), ing1, ing2 );
		assert TestTools.checkCollection( rev4.getReferencing(), ing2 );
		assert TestTools.checkCollection( rev5.getReferencing(), ing2 );
	}

	@Test
	public void testHistoryOfEdId2() {
		ListOwningEntity ing1 = getEntityManager().find( ListOwningEntity.class, ing1_id );
		ListOwningEntity ing2 = getEntityManager().find( ListOwningEntity.class, ing2_id );

		ListOwnedEntity rev1 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 1 );
		ListOwnedEntity rev2 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 2 );
		ListOwnedEntity rev3 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 3 );
		ListOwnedEntity rev4 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 4 );
		ListOwnedEntity rev5 = getAuditReader().find( ListOwnedEntity.class, ed2_id, 5 );

		assert rev1.getReferencing().equals( Collections.EMPTY_LIST );
		assert TestTools.checkCollection( rev2.getReferencing(), ing2 );
		assert TestTools.checkCollection( rev3.getReferencing(), ing1, ing2 );
		assert TestTools.checkCollection( rev4.getReferencing(), ing1, ing2 );
		assert TestTools.checkCollection( rev5.getReferencing(), ing2 );
	}

	@Test
	public void testHistoryOfEdIng1() {
		ListOwnedEntity ed1 = getEntityManager().find( ListOwnedEntity.class, ed1_id );
		ListOwnedEntity ed2 = getEntityManager().find( ListOwnedEntity.class, ed2_id );

		ListOwningEntity rev1 = getAuditReader().find( ListOwningEntity.class, ing1_id, 1 );
		ListOwningEntity rev2 = getAuditReader().find( ListOwningEntity.class, ing1_id, 2 );
		ListOwningEntity rev3 = getAuditReader().find( ListOwningEntity.class, ing1_id, 3 );
		ListOwningEntity rev4 = getAuditReader().find( ListOwningEntity.class, ing1_id, 4 );
		ListOwningEntity rev5 = getAuditReader().find( ListOwningEntity.class, ing1_id, 5 );

		assert rev1.getReferences().equals( Collections.EMPTY_LIST );
		assert TestTools.checkCollection( rev2.getReferences(), ed1 );
		assert TestTools.checkCollection( rev3.getReferences(), ed1, ed2 );
		assert TestTools.checkCollection( rev4.getReferences(), ed2 );
		assert rev5.getReferences().equals( Collections.EMPTY_LIST );
	}

	@Test
	public void testHistoryOfEdIng2() {
		ListOwnedEntity ed1 = getEntityManager().find( ListOwnedEntity.class, ed1_id );
		ListOwnedEntity ed2 = getEntityManager().find( ListOwnedEntity.class, ed2_id );

		ListOwningEntity rev1 = getAuditReader().find( ListOwningEntity.class, ing2_id, 1 );
		ListOwningEntity rev2 = getAuditReader().find( ListOwningEntity.class, ing2_id, 2 );
		ListOwningEntity rev3 = getAuditReader().find( ListOwningEntity.class, ing2_id, 3 );
		ListOwningEntity rev4 = getAuditReader().find( ListOwningEntity.class, ing2_id, 4 );
		ListOwningEntity rev5 = getAuditReader().find( ListOwningEntity.class, ing2_id, 5 );

		assert rev1.getReferences().equals( Collections.EMPTY_LIST );
		assert TestTools.checkCollection( rev2.getReferences(), ed1, ed2 );
		assert TestTools.checkCollection( rev3.getReferences(), ed1, ed2 );
		assert TestTools.checkCollection( rev4.getReferences(), ed1, ed2 );
		assert TestTools.checkCollection( rev5.getReferences(), ed1, ed2 );
	}
}
