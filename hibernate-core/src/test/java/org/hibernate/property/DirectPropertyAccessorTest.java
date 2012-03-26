/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
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
 *
 */
package org.hibernate.property;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Michael Rudolf
 */
public class DirectPropertyAccessorTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue( jiraKey="HHH-3718" )
	public void testDirectIdPropertyAccess() throws Exception {
		Session s = openSession();
		final Transaction transaction = s.beginTransaction();
		Item i = new Item();
		s.persist( i );
		Order o = new Order();
		o.setOrderNumber( 1 );
		o.getItems().add( i );
		s.persist( o );
		transaction.commit();
		s.clear();

		o = ( Order ) s.load( Order.class, 1 );
		assertFalse( Hibernate.isInitialized( o ) );
		o.getOrderNumber();
		// If you mapped with field access, any method call initializes the proxy
		assertTrue( Hibernate.isInitialized( o ) );
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Order.class,
				Item.class,
		};
	}
}
