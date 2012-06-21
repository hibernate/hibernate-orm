/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.collectionelement.ordered;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class ElementCollectionSortingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	public void testSortingElementCollectionSyntax() {
		Session session = openSession();
		session.beginTransaction();

		session.createQuery( "from Person p join fetch p.nickNamesAscendingNaturalSort" ).list();
		session.createQuery( "from Person p join fetch p.nickNamesDescendingNaturalSort" ).list();

		session.createQuery( "from Person p join fetch p.addressesAscendingNaturalSort" ).list();
		session.createQuery( "from Person p join fetch p.addressesDescendingNaturalSort" ).list();
		session.createQuery( "from Person p join fetch p.addressesCityAscendingSort" ).list();
		session.createQuery( "from Person p join fetch p.addressesCityDescendingSort" ).list();

		session.getTransaction().commit();
		session.close();
	}
}
