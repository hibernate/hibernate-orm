/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.relations;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.components.relations.NotAuditedManyToOneComponent;
import org.hibernate.orm.test.envers.entities.components.relations.NotAuditedManyToOneComponentTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotAuditedManyToOneInComponent extends BaseEnversJPAFunctionalTestCase {
	private Integer mtocte_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {NotAuditedManyToOneComponentTestEntity.class, UnversionedStrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// No revision
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		UnversionedStrTestEntity ste1 = new UnversionedStrTestEntity();
		ste1.setStr( "str1" );

		UnversionedStrTestEntity ste2 = new UnversionedStrTestEntity();
		ste2.setStr( "str2" );

		em.persist( ste1 );
		em.persist( ste2 );

		em.getTransaction().commit();

		// Revision 1
		em = getEntityManager();
		em.getTransaction().begin();

		NotAuditedManyToOneComponentTestEntity mtocte1 = new NotAuditedManyToOneComponentTestEntity(
				new NotAuditedManyToOneComponent( ste1, "data1" )
		);

		em.persist( mtocte1 );

		em.getTransaction().commit();

		// No revision
		em = getEntityManager();
		em.getTransaction().begin();

		mtocte1 = em.find( NotAuditedManyToOneComponentTestEntity.class, mtocte1.getId() );
		mtocte1.getComp1().setEntity( ste2 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		mtocte1 = em.find( NotAuditedManyToOneComponentTestEntity.class, mtocte1.getId() );
		mtocte1.getComp1().setData( "data2" );

		em.getTransaction().commit();

		mtocte_id1 = mtocte1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 )
				.equals( getAuditReader().getRevisions( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		NotAuditedManyToOneComponentTestEntity ver1 = new NotAuditedManyToOneComponentTestEntity(
				mtocte_id1,
				new NotAuditedManyToOneComponent( null, "data1" )
		);
		NotAuditedManyToOneComponentTestEntity ver2 = new NotAuditedManyToOneComponentTestEntity(
				mtocte_id1,
				new NotAuditedManyToOneComponent( null, "data2" )
		);

		assert getAuditReader().find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1, 1 ).equals( ver1 );
		assert getAuditReader().find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1, 2 ).equals( ver2 );
	}
}
