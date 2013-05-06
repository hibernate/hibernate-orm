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
public class Serializables extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SerializableTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		SerializableTestEntity ste = new SerializableTestEntity( new SerObject( "d1" ) );
		em.persist( ste );
		id1 = ste.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		ste = em.find( SerializableTestEntity.class, id1 );
		ste.setObj( new SerObject( "d2" ) );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SerializableTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		SerializableTestEntity ver1 = new SerializableTestEntity( id1, new SerObject( "d1" ) );
		SerializableTestEntity ver2 = new SerializableTestEntity( id1, new SerObject( "d2" ) );

		assert getAuditReader().find( SerializableTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( SerializableTestEntity.class, id1, 2 ).equals( ver2 );
	}
}