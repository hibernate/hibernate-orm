/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.keymanytoone.unidir.ondelete;

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
		return new String[] { "keymanytoone/unidir/ondelete/Mapping.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7807" )
	public void testComponentCascadeRemoval() {
		Session session = openSession();

		session.getTransaction().begin();
		Customer customer = new Customer( "Lukasz" );
		Order order1 = new Order( new Order.Id( customer, 1L ) );
		order1.setItem( "laptop" );
		Order order2 = new Order( new Order.Id( customer, 2L ) );
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
