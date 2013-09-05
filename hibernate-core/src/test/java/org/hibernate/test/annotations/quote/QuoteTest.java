/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class QuoteTest extends BaseCoreFunctionalTestCase {
	
	@Test
	public void testQuoteManytoMany() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		User u = new User();
		s.persist( u );
		Role r = new Role();
		s.persist( r );
		u.getRoles().add( r );
		s.flush();
		s.clear();
		u = (User) s.get( User.class, u.getId() );
		assertEquals( 1, u.getRoles().size() );
		tx.rollback();
		String role = User.class.getName() + ".roles";
		assertEquals( "User_Role", configuration().getCollectionMapping( role ).getCollectionTable().getName() );
		s.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8464")
	public void testDoubleQuoteJoinColumn() {
		Session s = openSession();
		s.getTransaction().begin();
		User user = new User();
		House house = new House();
		user.setHouse( house );
		s.persist( house );
		s.persist( user );
		s.getTransaction().commit();
		s.clear();
		
		s = openSession();
		s.getTransaction().begin();
		user = (User) s.get( User.class, user.getId() );
		assertNotNull( user );
		assertNotNull( user.getHouse() );
		// seems trivial, but if quoting normalization worked on the join column, these should all be the same
		assertEquals( user.getHouse().getId(), user.getHouse1() );
		assertEquals( user.getHouse().getId(), user.getHouse2() );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
				Role.class,
				Phone.class,
				House.class
		};
	}
}
