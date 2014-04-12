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
package org.hibernate.envers.test.integration.accesstype;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@FailureExpectedWithNewMetamodel( message = "Building SessionFactory for envers with LenientPersistentAttributeMemberResolver.INSTANCE is not supported yet.")
public class MixedAccessType extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MixedAccessTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		MixedAccessTypeEntity mate = new MixedAccessTypeEntity( "data" );
		em.persist( mate );
		id1 = mate.readId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		mate = em.find( MixedAccessTypeEntity.class, id1 );
		mate.writeData( "data2" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( MixedAccessTypeEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		MixedAccessTypeEntity ver1 = new MixedAccessTypeEntity( id1, "data" );
		MixedAccessTypeEntity ver2 = new MixedAccessTypeEntity( id1, "data2" );

		MixedAccessTypeEntity rev1 = getAuditReader().find( MixedAccessTypeEntity.class, id1, 1 );
		MixedAccessTypeEntity rev2 = getAuditReader().find( MixedAccessTypeEntity.class, id1, 2 );

		assert rev1.isDataSet();
		assert rev2.isDataSet();

		assert rev1.equals( ver1 );
		assert rev2.equals( ver2 );
	}
}