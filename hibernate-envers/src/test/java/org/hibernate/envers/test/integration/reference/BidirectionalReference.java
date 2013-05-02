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
package org.hibernate.envers.test.integration.reference;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BidirectionalReference extends BaseEnversJPAFunctionalTestCase {
	private Long set1_id;
	private Long set2_id;

	private Long g1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {GreetingPO.class, GreetingSetPO.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		GreetingSetPO set1 = new GreetingSetPO();
		set1.setName( "a1" );

		GreetingSetPO set2 = new GreetingSetPO();
		set2.setName( "a2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( set1 );
		em.persist( set2 );

		set1_id = set1.getId();
		set2_id = set2.getId();

		em.getTransaction().commit();
		em.clear();

		// Revision 2
		em.getTransaction().begin();

		GreetingPO g1 = new GreetingPO();
		g1.setGreeting( "g1" );
		g1.setGreetingSet( em.getReference( GreetingSetPO.class, set1_id ) );

		em.persist( g1 );
		g1_id = g1.getId();

		em.getTransaction().commit();
		em.clear();

		// Revision 3
		em.getTransaction().begin();

		g1 = em.find( GreetingPO.class, g1_id );

		g1.setGreetingSet( em.getReference( GreetingSetPO.class, set2_id ) );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 2, 3 ).equals( getAuditReader().getRevisions( GreetingPO.class, g1_id ) );

		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( GreetingSetPO.class, set1_id ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( GreetingSetPO.class, set2_id ) );
	}

	@Test
	public void testHistoryOfG1() {
		GreetingPO rev1 = getAuditReader().find( GreetingPO.class, g1_id, 1 );
		GreetingPO rev2 = getAuditReader().find( GreetingPO.class, g1_id, 2 );
		GreetingPO rev3 = getAuditReader().find( GreetingPO.class, g1_id, 3 );

		assert rev1 == null;
		assert rev2.getGreetingSet().getName().equals( "a1" );
		assert rev3.getGreetingSet().getName().equals( "a2" );
	}

	@Test
	public void testHistoryOfSet1() {
		GreetingSetPO rev1 = getAuditReader().find( GreetingSetPO.class, set1_id, 1 );
		GreetingSetPO rev2 = getAuditReader().find( GreetingSetPO.class, set1_id, 2 );
		GreetingSetPO rev3 = getAuditReader().find( GreetingSetPO.class, set1_id, 3 );

		assert rev1.getName().equals( "a1" );
		assert rev2.getName().equals( "a1" );
		assert rev3.getName().equals( "a1" );

		GreetingPO g1 = new GreetingPO();
		g1.setId( g1_id );
		g1.setGreeting( "g1" );

		assert rev1.getGreetings().size() == 0;
		assert rev2.getGreetings().equals( TestTools.makeSet( g1 ) );
		assert rev3.getGreetings().size() == 0;
	}

	@Test
	public void testHistoryOfSet2() {
		GreetingSetPO rev1 = getAuditReader().find( GreetingSetPO.class, set2_id, 1 );
		GreetingSetPO rev2 = getAuditReader().find( GreetingSetPO.class, set2_id, 2 );
		GreetingSetPO rev3 = getAuditReader().find( GreetingSetPO.class, set2_id, 3 );

		assert rev1.getName().equals( "a2" );
		assert rev2.getName().equals( "a2" );
		assert rev3.getName().equals( "a2" );

		GreetingPO g1 = new GreetingPO();
		g1.setId( g1_id );
		g1.setGreeting( "g1" );

		assert rev1.getGreetings().size() == 0;
		assert rev2.getGreetings().size() == 0;
		assert rev3.getGreetings().equals( TestTools.makeSet( g1 ) );
	}
}