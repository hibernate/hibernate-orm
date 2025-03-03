/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator.joined;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.After;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11133")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/mapping/inheritance/discriminator/joined/JoinedSubclassInheritance.hbm.xml"
)
@SessionFactory
public class JoinedSubclassInheritanceTest {

	@After
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from ChildEntity" ).executeUpdate()
		);
	}

	@Test
	public void testConfiguredDiscriminatorValue(SessionFactoryScope scope) {
		final ChildEntity childEntity = new ChildEntity( 1, "Child" );
		scope.inTransaction( session -> session.persist( childEntity ) );

		scope.inTransaction( session -> {
			ChildEntity ce = session.find( ChildEntity.class, 1 );
			assertThat( ce.getType(), is( "ce" ) );
		} );
	}

}
