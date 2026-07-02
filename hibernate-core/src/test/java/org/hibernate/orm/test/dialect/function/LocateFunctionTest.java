/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;


import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the locate() function, some dialects (like PostgreSQL) uses
 * position() and substring() under the hood.
 */
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class LocateFunctionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityOfBasics entity = new EntityOfBasics();
			entity.setId( 1 );
			entity.setTheString( "hello world" );
			session.persist( entity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLocateTwoArgsFound(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Integer result = session.createQuery(
					"select locate('world', 'hello world')", Integer.class
			).getSingleResult();
			assertEquals( 7, result );
		} );
	}

	@Test
	public void testLocateTwoArgsNotFound(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Integer result = session.createQuery(
					"select locate('xyz', 'hello world')", Integer.class
			).getSingleResult();
			assertEquals( 0, result );
		} );
	}

	@Test
	public void testLocateThreeArgsFound(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Integer result = session.createQuery(
					"select locate('o', 'hello world', 6)", Integer.class
			).getSingleResult();
			assertEquals( 8, result );
		} );
	}

	@Test
	public void testLocateThreeArgsNotFound(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Integer result = session.createQuery(
					"select locate('xyz', 'hello world', 3)", Integer.class
			).getSingleResult();
			assertEquals( 0, result );
		} );
	}

	@Test
	public void testLocateThreeArgsStartAtMatch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Integer result = session.createQuery(
					"select locate('world', 'hello world', 7)", Integer.class
			).getSingleResult();
			assertEquals( 7, result );
		} );
	}
}
