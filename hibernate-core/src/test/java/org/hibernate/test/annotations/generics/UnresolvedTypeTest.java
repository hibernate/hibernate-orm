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
