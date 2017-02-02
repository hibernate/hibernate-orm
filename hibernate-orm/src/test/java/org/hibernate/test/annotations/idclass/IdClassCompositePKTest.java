/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.test.annotations.idclass;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
public class IdClassCompositePKTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testEntityMappningPropertiesAreNotIgnored() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		DomainAdmin da = new DomainAdmin();
		da.setAdminUser( "admin" );
		da.setDomainName( "org" );

		s.persist( da );
		Query q = s.getNamedQuery( "DomainAdmin.testQuery" );
		assertEquals( 1, q.list().size() );

		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { DomainAdmin.class };
	}
}
