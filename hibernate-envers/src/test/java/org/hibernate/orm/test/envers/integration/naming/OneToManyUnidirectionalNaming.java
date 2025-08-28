/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.mapping.Column;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class OneToManyUnidirectionalNaming extends BaseEnversJPAFunctionalTestCase {
	private Integer uni1_id;
	private Integer str1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {DetachedNamingTestEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		DetachedNamingTestEntity uni1 = new DetachedNamingTestEntity( 1, "data1" );
		StrTestEntity str1 = new StrTestEntity( "str1" );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		uni1.setCollection( new HashSet<StrTestEntity>() );
		em.persist( uni1 );
		em.persist( str1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		uni1 = em.find( DetachedNamingTestEntity.class, uni1.getId() );
		str1 = em.find( StrTestEntity.class, str1.getId() );
		uni1.getCollection().add( str1 );

		em.getTransaction().commit();

		//

		uni1_id = uni1.getId();
		str1_id = str1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( DetachedNamingTestEntity.class, uni1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, str1_id ) );
	}

	@Test
	public void testHistoryOfUniId1() {
		StrTestEntity str1 = getEntityManager().find( StrTestEntity.class, str1_id );

		DetachedNamingTestEntity rev1 = getAuditReader().find( DetachedNamingTestEntity.class, uni1_id, 1 );
		DetachedNamingTestEntity rev2 = getAuditReader().find( DetachedNamingTestEntity.class, uni1_id, 2 );

		assert rev1.getCollection().equals( TestTools.makeSet() );
		assert rev2.getCollection().equals( TestTools.makeSet( str1 ) );

		assert "data1".equals( rev1.getData() );
		assert "data1".equals( rev2.getData() );
	}

	private final static String MIDDLE_VERSIONS_ENTITY_NAME = "UNI_NAMING_TEST_AUD";

	@Test
	public void testTableName() {
		assert MIDDLE_VERSIONS_ENTITY_NAME.equals(
				metadata().getEntityBinding( MIDDLE_VERSIONS_ENTITY_NAME ).getTable().getName()
		);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testJoinColumnName() {
		Iterator<Column> columns =
				metadata().getEntityBinding( MIDDLE_VERSIONS_ENTITY_NAME ).getTable().getColumns().iterator();

		boolean id1Found = false;
		boolean id2Found = false;

		while ( columns.hasNext() ) {
			Column column = columns.next();
			if ( "ID_1".equals( column.getName() ) ) {
				id1Found = true;
			}

			if ( "ID_2".equals( column.getName() ) ) {
				id2Found = true;
			}
		}

		assert id1Found && id2Found;
	}
}
