/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.naming;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Value;

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
				getMetadata().getEntityBinding( MIDDLE_VERSIONS_ENTITY_NAME ).getPrimaryTableName()
		);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testJoinColumnName() {

		boolean id1Found = false;
		boolean id2Found = false;

		final List<Value> values = getMetadata().getEntityBinding( MIDDLE_VERSIONS_ENTITY_NAME ).getPrimaryTable().values();
		for ( Value value : values ) {
			Column column = (Column) value;
			if ( "ID_1".equals( column.getColumnName().getText() ) ) {
				id1Found = true;
			}

			if ( "ID_2".equals( column.getColumnName().getText() ) ) {
				id2Found = true;
			}
		}

		assert id1Found && id2Found;
	}
}
