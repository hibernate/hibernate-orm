/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BidirectionalMapKey extends BaseEnversJPAFunctionalTestCase {
	private Integer ed_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {RefIngMapKeyEntity.class, RefEdMapKeyEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 (intialy 1 relation: ing1 -> ed)
		em.getTransaction().begin();

		RefEdMapKeyEntity ed = new RefEdMapKeyEntity();

		em.persist( ed );

		RefIngMapKeyEntity ing1 = new RefIngMapKeyEntity();
		ing1.setData( "a" );
		ing1.setReference( ed );

		RefIngMapKeyEntity ing2 = new RefIngMapKeyEntity();
		ing2.setData( "b" );

		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2 (adding second relation: ing2 -> ed)
		em.getTransaction().begin();

		ed = em.find( RefEdMapKeyEntity.class, ed.getId() );
		ing2 = em.find( RefIngMapKeyEntity.class, ing2.getId() );

		ing2.setReference( ed );

		em.getTransaction().commit();

		//

		ed_id = ed.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( RefEdMapKeyEntity.class, ed_id ) );

		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( RefIngMapKeyEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( RefIngMapKeyEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEd() {
		RefIngMapKeyEntity ing1 = getEntityManager().find( RefIngMapKeyEntity.class, ing1_id );
		RefIngMapKeyEntity ing2 = getEntityManager().find( RefIngMapKeyEntity.class, ing2_id );

		RefEdMapKeyEntity rev1 = getAuditReader().find( RefEdMapKeyEntity.class, ed_id, 1 );
		RefEdMapKeyEntity rev2 = getAuditReader().find( RefEdMapKeyEntity.class, ed_id, 2 );

		assert rev1.getIdmap().equals( TestTools.makeMap( "a", ing1 ) );
		assert rev2.getIdmap().equals( TestTools.makeMap( "a", ing1, "b", ing2 ) );
	}
}
