/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;
import java.util.HashSet;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.SetRefCollEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DataChangesDetachedSet extends BaseEnversJPAFunctionalTestCase {
	private Integer str1_id;

	private Integer coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, SetRefCollEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StrTestEntity str1 = new StrTestEntity( "str1" );

		SetRefCollEntity coll1 = new SetRefCollEntity( 3, "coll1" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( str1 );

		coll1.setCollection( new HashSet<StrTestEntity>() );
		em.persist( coll1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		str1 = em.find( StrTestEntity.class, str1.getId() );
		coll1 = em.find( SetRefCollEntity.class, coll1.getId() );

		coll1.getCollection().add( str1 );
		coll1.setData( "coll2" );

		em.getTransaction().commit();

		//

		str1_id = str1.getId();

		coll1_id = coll1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SetRefCollEntity.class, coll1_id ) );

		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, str1_id ) );
	}

	@Test
	public void testHistoryOfColl1() {
		StrTestEntity str1 = getEntityManager().find( StrTestEntity.class, str1_id );

		SetRefCollEntity rev1 = getAuditReader().find( SetRefCollEntity.class, coll1_id, 1 );
		SetRefCollEntity rev2 = getAuditReader().find( SetRefCollEntity.class, coll1_id, 2 );

		assert rev1.getCollection().equals( TestTools.makeSet() );
		assert rev2.getCollection().equals( TestTools.makeSet( str1 ) );

		assert "coll1".equals( rev1.getData() );
		assert "coll2".equals( rev2.getData() );
	}
}
