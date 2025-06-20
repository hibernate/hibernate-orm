/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.propertyref.lazy;

import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-565")
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/collection/propertyref/lazy/User.hbm.xml",
				"org/hibernate/orm/test/collection/propertyref/lazy/Mail.hbm.xml"
		}
)
@SessionFactory
public class PropertyRefTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		User user = new User( "test" );
		scope.inTransaction(
				session -> {
					user.addMail( "test" );
					user.addMail( "test" );
					session.persist( user );
				}
		);

		scope.inTransaction(
				session -> {
					final User u = session.get(
							User.class,
							user.getId()
					);

					final Set<Mail> mail = u.getMail();
					assertFalse( Hibernate.isInitialized( mail ) );
					assertEquals( 2, mail.size() );

					final String alias = mail.iterator().next().getAlias();
					assertEquals( "test", alias );
				}
		);
	}

}
