/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.QueryException;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * DB2 has 2 functions for getting a substring: "substr" and "substring"
 *
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(DB2Dialect.class)
@DomainModel(annotatedClasses = DB297SubStringFunctionsTest.AnEntity.class)
@SessionFactory(useCollectingStatementInspector = true)
public class DB297SubStringFunctionsTest {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			AnEntity anEntity = new AnEntity();
			anEntity.description = "A very long, boring description.";
			session.persist( anEntity );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-11957")
	public void testSubstringWithStringUnits(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			String value = session.createQuery(
					"select substring( e.description, 21, 11, sql('octets') ) from AnEntity e",
					String.class
			).uniqueResult();
			assertThat( value ).isEqualTo( "description" );
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( "substring(" );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( "octets" );
	}

	@Test
	@JiraKey( value = "HHH-11957")
	public void testSubstringWithoutStringUnits(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			String value = session.createQuery(
					"select substring( e.description, 21, 11 ) from AnEntity e",
					String.class
			).uniqueResult();
			assertThat( value ).isEqualTo( "description" );
		} );
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( "substring(" );
	}

	@Test
	@JiraKey( value = "HHH-11957")
	public void testSubstrWithStringUnits(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createQuery(
						"select substr( e.description, 21, 11, sql('octets') ) from AnEntity e",
						String.class
				).uniqueResult();
				fail( "Should have failed because substr cannot be used with string units." );
			}
			catch (IllegalArgumentException expected) {
				assertThat( expected.getCause() ).isInstanceOf(  QueryException.class );
			}
		} );
	}

	@Test
	@JiraKey( value = "HHH-11957")
	public void testSubstrWithoutStringUnits(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			String value = session.createQuery(
					"select substr( e.description, 21, 11 ) from AnEntity e",
					String.class
			).uniqueResult();
			assertThat( value ).isEqualTo( "description" );
		} );
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( "substr(" );
	}

	@Entity(name="AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private long id;
		private String description;
	}

}
