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

package org.hibernate.envers.test.integration.inheritance.joined.primarykeyjoin;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.inheritance.joined.ParentEntity;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChildPrimaryKeyJoinAuditing extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildPrimaryKeyJoinEntity.class, ParentEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		id1 = 1;

		// Rev 1
		em.getTransaction().begin();
		ChildPrimaryKeyJoinEntity ce = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );
		em.persist( ce );
		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();
		ce = em.find( ChildPrimaryKeyJoinEntity.class, id1 );
		ce.setData( "y" );
		ce.setNumVal( 2l );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ChildPrimaryKeyJoinEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfChildId1() {
		ChildPrimaryKeyJoinEntity ver1 = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );
		ChildPrimaryKeyJoinEntity ver2 = new ChildPrimaryKeyJoinEntity( id1, "y", 2l );

		assert getAuditReader().find( ChildPrimaryKeyJoinEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( ChildPrimaryKeyJoinEntity.class, id1, 2 ).equals( ver2 );

		assert getAuditReader().find( ParentEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( ParentEntity.class, id1, 2 ).equals( ver2 );
	}

	@Test
	public void testPolymorphicQuery() {
		ChildPrimaryKeyJoinEntity childVer1 = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );

		assert getAuditReader().createQuery()
				.forEntitiesAtRevision( ChildPrimaryKeyJoinEntity.class, 1 )
				.getSingleResult()
				.equals( childVer1 );

		assert getAuditReader().createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult()
				.equals( childVer1 );
	}

	@Test
	@FailureExpectedWithNewMetamodel( message = "@PrimaryKeyJoinColumn does not work on sub-entity." )
	public void testChildIdColumnName() {
		Assert.assertEquals(
				"other_id",
				( (Column) getMetadata()
						.getEntityBinding(
								"org.hibernate.envers.test.integration.inheritance.joined.primarykeyjoin.ChildPrimaryKeyJoinEntity_AUD"
						)
						.getHierarchyDetails()
						.getEntityIdentifier()
						.getEntityIdentifierBinding()
						.getAttributeBinding()
						.getValues()
						.get( 0 ) ).getColumnName().getText()
		);
	}
}