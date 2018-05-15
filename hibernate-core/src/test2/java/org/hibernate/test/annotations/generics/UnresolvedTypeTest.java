/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.generics;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Paolo Perrotta
 */
public class UnresolvedTypeTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testAcceptsUnresolvedPropertyTypesIfATargetEntityIsExplicitlySet() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Gene item = new Gene();
		s.persist( item );
		s.flush();
		tx.rollback();
		s.close();
	}

	@Test
	public void testAcceptsUnresolvedPropertyTypesIfATypeExplicitlySet() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Gene item = new Gene();
		item.setState( State.DORMANT );
		s.persist( item );
		s.flush();
		s.clear();
		item = (Gene) s.get( Gene.class, item.getId() );
		assertEquals( State.DORMANT, item.getState() );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Gene.class,
				DNA.class
		};
	}
}
