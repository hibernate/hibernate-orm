/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
