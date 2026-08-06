/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany.ordered;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/manytomany/ordered/UserGroupManyToMany.orm.xml"
)
@SessionFactory
@JiraKey( "HHH-20764" )
public class ManyToManyOrderByColumnOrmXmlTest {


	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					OrderedUser grace = new OrderedUser( "grace" );
					OrderedGroup admins = new OrderedGroup( "admins" );
					OrderedGroup users = new OrderedGroup( "users" );
					grace.getGroups().add( users );
					grace.getGroups().add( admins );
					session.persist( grace );
					session.persist( users );
					session.persist( admins );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testManyToManyOrderByJoinColumnFromOrmXml(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					OrderedUser grace = session.createQuery(
									"from OrderedUser u join fetch u.groups where u.name = :name",
									OrderedUser.class
							)
							.setParameter( "name", "grace" )
							.uniqueResult();

					assertTrue( Hibernate.isInitialized( grace.getGroups() ) );
					assertEquals( 2, grace.getGroups().size() );
					assertEquals( "admins", grace.getGroups().get( 0 ).getName() );
					assertEquals( "users", grace.getGroups().get( 1 ).getName() );
				}
		);
	}
}
