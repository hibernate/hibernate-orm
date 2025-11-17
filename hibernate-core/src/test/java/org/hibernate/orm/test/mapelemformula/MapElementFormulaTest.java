/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapelemformula;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/mapelemformula/UserGroup.hbm.xml"
)
@SessionFactory
public class MapElementFormulaTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User gavin = new User( "gavin", "secret" );
					User turin = new User( "turin", "tiger" );
					Group g = new Group( "users" );
					g.getUsers().put( "Gavin", gavin );
					g.getUsers().put( "Turin", turin );
					session.persist( g );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testManyToManyFormula(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Group g = session.get( Group.class, "users" );
					assertEquals( 2, g.getUsers().size() );
					g.getUsers().remove( "Turin" );
				}
		);
	}

}
