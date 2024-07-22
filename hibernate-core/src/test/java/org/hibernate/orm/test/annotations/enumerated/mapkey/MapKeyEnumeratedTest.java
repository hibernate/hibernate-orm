/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.enumerated.mapkey;

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
		User user = new User("User1", SocialNetwork.STUB_NETWORK_NAME, "facebookId");
		s.persist( user );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.get( User.class, user.getId() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.get( User.class, user.getId() );
		s.remove( user );
		s.getTransaction().commit();
		s.close();
	}
}
