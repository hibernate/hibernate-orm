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
package org.hibernate.envers.test.integration.ids;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Date;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.CompositeDateIdTestEntity;
import org.hibernate.envers.test.entities.ids.DateEmbId;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CompositeDateId extends BaseEnversJPAFunctionalTestCase {
	private DateEmbId id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CompositeDateIdTestEntity.class, DateEmbId.class};
	}

	@Test
	@Priority(10)
	public void initData() {

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		CompositeDateIdTestEntity dite = new CompositeDateIdTestEntity( new DateEmbId( new Date(), new Date() ), "x" );
		em.persist( dite );

		id1 = dite.getId();

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		dite = em.find( CompositeDateIdTestEntity.class, id1 );
		dite.setStr1( "y" );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( CompositeDateIdTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		CompositeDateIdTestEntity ver1 = new CompositeDateIdTestEntity( id1, "x" );
		CompositeDateIdTestEntity ver2 = new CompositeDateIdTestEntity( id1, "y" );

		assert getAuditReader().find( CompositeDateIdTestEntity.class, id1, 1 ).getStr1().equals( "x" );
		assert getAuditReader().find( CompositeDateIdTestEntity.class, id1, 2 ).getStr1().equals( "y" );
	}
}