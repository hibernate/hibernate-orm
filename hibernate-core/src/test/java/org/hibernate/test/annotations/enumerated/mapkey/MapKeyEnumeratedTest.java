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
package org.hibernate.test.annotations.enumerated.mapkey;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class MapKeyEnumeratedTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, SocialNetworkProfile.class };
	}

	@Test
	public void testMapKeyEnumerated() {
		Session s = openSession();
		s.beginTransaction();
		User user = new User(SocialNetwork.STUB_NETWORK_NAME, "facebookId");
		s.save( user );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = (User) s.get( User.class, user.getId() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = (User) s.get( User.class, user.getId() );
		s.delete( user );
		s.getTransaction().commit();
		s.close();
	}
}
