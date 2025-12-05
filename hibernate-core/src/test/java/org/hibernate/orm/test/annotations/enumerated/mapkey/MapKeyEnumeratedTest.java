/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.enumerated.mapkey;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				User.class,
				SocialNetworkProfile.class
		}
)
@SessionFactory
public class MapKeyEnumeratedTest {

	@Test
	public void testMapKeyEnumerated(SessionFactoryScope scope) {
		User u = new User( "User1", SocialNetwork.STUB_NETWORK_NAME, "facebookId" );
		scope.inTransaction(
				session -> session.persist( u )

		);

		scope.inTransaction(
				session ->
						session.find( User.class, u.getId() )
		);

		scope.inTransaction(
				session -> {
					User user = session.find( User.class, u.getId() );
					session.remove( user );
				}
		);
	}
}
