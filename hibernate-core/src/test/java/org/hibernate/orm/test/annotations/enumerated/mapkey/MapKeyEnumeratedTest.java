/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
