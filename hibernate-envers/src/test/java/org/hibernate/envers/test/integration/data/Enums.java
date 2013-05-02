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
package org.hibernate.envers.test.integration.data;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Enums extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {EnumTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		EnumTestEntity ete = new EnumTestEntity( EnumTestEntity.E1.X, EnumTestEntity.E2.A );
		em.persist( ete );
		id1 = ete.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		ete = em.find( EnumTestEntity.class, id1 );
		ete.setEnum1( EnumTestEntity.E1.Y );
		ete.setEnum2( EnumTestEntity.E2.B );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( EnumTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		EnumTestEntity ver1 = new EnumTestEntity( id1, EnumTestEntity.E1.X, EnumTestEntity.E2.A );
		EnumTestEntity ver2 = new EnumTestEntity( id1, EnumTestEntity.E1.Y, EnumTestEntity.E2.B );

		assert getAuditReader().find( EnumTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( EnumTestEntity.class, id1, 2 ).equals( ver2 );
	}
}