/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import java.util.Arrays;
import java.util.Iterator;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.mapping.Selectable;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinNaming extends BaseEnversJPAFunctionalTestCase {
	private Integer ed_id1;
	private Integer ed_id2;
	private Integer ing_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {JoinNamingRefEdEntity.class, JoinNamingRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		JoinNamingRefEdEntity ed1 = new JoinNamingRefEdEntity( "data1" );
		JoinNamingRefEdEntity ed2 = new JoinNamingRefEdEntity( "data2" );

		JoinNamingRefIngEntity ing1 = new JoinNamingRefIngEntity( "x", ed1 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ed2 = em.find( JoinNamingRefEdEntity.class, ed2.getId() );

		ing1 = em.find( JoinNamingRefIngEntity.class, ing1.getId() );
		ing1.setData( "y" );
		ing1.setReference( ed2 );

		em.getTransaction().commit();

		//

		ed_id1 = ed1.getId();
		ed_id2 = ed2.getId();
		ing_id1 = ing1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( JoinNamingRefEdEntity.class, ed_id1 ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( JoinNamingRefEdEntity.class, ed_id2 ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( JoinNamingRefIngEntity.class, ing_id1 ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity( ed_id1, "data1" );

		assert getAuditReader().find( JoinNamingRefEdEntity.class, ed_id1, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinNamingRefEdEntity.class, ed_id1, 2 ).equals( ver1 );
	}

	@Test
	public void testHistoryOfEdId2() {
		JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity( ed_id2, "data2" );

		assert getAuditReader().find( JoinNamingRefEdEntity.class, ed_id2, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinNamingRefEdEntity.class, ed_id2, 2 ).equals( ver1 );
	}

	@Test
	public void testHistoryOfIngId1() {
		JoinNamingRefIngEntity ver1 = new JoinNamingRefIngEntity( ing_id1, "x", null );
		JoinNamingRefIngEntity ver2 = new JoinNamingRefIngEntity( ing_id1, "y", null );

		assert getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 1 ).equals( ver1 );
		assert getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 2 ).equals( ver2 );

		assert getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 1 ).getReference().equals(
				new JoinNamingRefEdEntity( ed_id1, "data1" )
		);
		assert getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 2 ).getReference().equals(
				new JoinNamingRefEdEntity( ed_id2, "data2" )
		);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJoinColumnName() {
		Iterator<Selectable> columns = metadata().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.naming.JoinNamingRefIngEntity_AUD"
		).getProperty( "reference_id" ).getSelectables().iterator();
		assertTrue( columns.hasNext() );
		assertEquals( "jnree_column_reference", columns.next().getText() );
		assertFalse( columns.hasNext() );
	}
}
