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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class FieldAccessType extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {FieldAccessTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		FieldAccessTypeEntity fate = new FieldAccessTypeEntity( "data" );
		em.persist( fate );
		id1 = fate.readId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		fate = em.find( FieldAccessTypeEntity.class, id1 );
		fate.writeData( "data2" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( FieldAccessTypeEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		FieldAccessTypeEntity ver1 = new FieldAccessTypeEntity( id1, "data" );
		FieldAccessTypeEntity ver2 = new FieldAccessTypeEntity( id1, "data2" );
		Assert.assertEquals( ver1, getAuditReader().find( FieldAccessTypeEntity.class, id1, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( FieldAccessTypeEntity.class, id1, 2 ) );
	}
}