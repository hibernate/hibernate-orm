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
import org.hibernate.orm.test.envers.entities.onetomany.detached.SetJoinColumnRefCollEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicJoinColumnSet extends BaseEnversJPAFunctionalTestCase {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, SetJoinColumnRefCollEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StrTestEntity str1 = new StrTestEntity( "str1" );
		StrTestEntity str2 = new StrTestEntity( "str2" );

		SetJoinColumnRefCollEntity coll1 = new SetJoinColumnRefCollEntity( 3, "coll1" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( str1 );
		em.persist( str2 );

		coll1.setCollection( new HashSet<StrTestEntity>() );
		coll1.getCollection().add( str1 );
		em.persist( coll1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		str2 = em.find( StrTestEntity.class, str2.getId() );
		coll1 = em.find( SetJoinColumnRefCollEntity.class, coll1.getId() );

		coll1.getCollection().add( str2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		str1 = em.find( StrTestEntity.class, str1.getId() );
		coll1 = em.find( SetJoinColumnRefCollEntity.class, coll1.getId() );

		coll1.getCollection().remove( str1 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		coll1 = em.find( SetJoinColumnRefCollEntity.class, coll1.getId() );

		coll1.getCollection().clear();

		em.getTransaction().commit();

		//

		str1_id = str1.getId();
		str2_id = str2.getId();

		coll1_id = coll1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3, 4 ).equals(
				getAuditReader().getRevisions(
						SetJoinColumnRefCollEntity.class,
						coll1_id
				)
		);

		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, str1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, str2_id ) );
	}

	@Test
	public void testHistoryOfColl1() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		StrTestEntity str1 = entityManager.find( StrTestEntity.class, str1_id );
		StrTestEntity str2 = entityManager.find( StrTestEntity.class, str2_id );

		entityManager.getTransaction().commit();

		SetJoinColumnRefCollEntity rev1 = getAuditReader().find( SetJoinColumnRefCollEntity.class, coll1_id, 1 );
		SetJoinColumnRefCollEntity rev2 = getAuditReader().find( SetJoinColumnRefCollEntity.class, coll1_id, 2 );
		SetJoinColumnRefCollEntity rev3 = getAuditReader().find( SetJoinColumnRefCollEntity.class, coll1_id, 3 );
		SetJoinColumnRefCollEntity rev4 = getAuditReader().find( SetJoinColumnRefCollEntity.class, coll1_id, 4 );

		assert rev1.getCollection().equals( TestTools.makeSet( str1 ) );
		assert rev2.getCollection().equals( TestTools.makeSet( str1, str2 ) );
		assert rev3.getCollection().equals( TestTools.makeSet( str2 ) );
		assert rev4.getCollection().equals( TestTools.makeSet() );

		assert "coll1".equals( rev1.getData() );
		assert "coll1".equals( rev2.getData() );
		assert "coll1".equals( rev3.getData() );
		assert "coll1".equals( rev4.getData() );
	}
}
