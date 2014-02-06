/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.keymanytoone.bidir.ondelete;

import org.hibernate.Session;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialectFeature(DialectChecks.SupportsCascadeDeleteCheck.class)
public class KeyManyToOneCascadeDeleteTest extends BaseCoreFunctionalTestCase {
	@Override
    public String[] getMappings() {
		return new String[] { "keymanytoone/bidir/ondelete/Mapping.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7807" )
	public void testEmbeddedCascadeRemoval() {
		Session session = openSession();

		session.getTransaction().begin();
		Customer customer = new Customer( "Lukasz" );
		Order order1 = new Order( customer, 1L );
		order1.setItem( "laptop" );
		Order order2 = new Order( customer, 2L );
		order2.setItem( "printer" );
		session.save( customer );
		session.save( order1 );
		session.save( order2 );
		session.getTransaction().commit();

		// Removing customer cascades to associated orders.
		session.getTransaction().begin();
		customer = (Customer) session.get( Customer.class, customer.getId() );
		session.delete( customer );
		session.getTransaction().commit();

		session.getTransaction().begin();
		Assert.assertEquals( "0", session.createQuery( "select count(*) from Customer" ).uniqueResult().toString() );
		Assert.assertEquals( "0", session.createQuery( "select count(*) from Order" ).uniqueResult().toString() );
		session.getTransaction().commit();

		session.close();
	}
}
