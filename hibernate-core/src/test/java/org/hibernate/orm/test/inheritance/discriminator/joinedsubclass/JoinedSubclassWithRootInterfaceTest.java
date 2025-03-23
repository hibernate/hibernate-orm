/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/inheritance/discriminator/joinedsubclass/TestEntity.hbm.xml"
)
@SessionFactory
public class JoinedSubclassWithRootInterfaceTest {

	@Test
	@JiraKey(value = "HHH-11554")
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntityImpl testEntity = new TestEntityImpl();
			testEntity.setId( 1 );
			session.persist( testEntity );
		} );
	}
}
