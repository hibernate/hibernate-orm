/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.ExplicitSqlStringGenerationContext;
import org.hibernate.query.sql.internal.SQLQueryParser;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SQLQueryParser}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SQLQueryParserUnitTests {

	@Test
	@DomainModel
	@SessionFactory
	@RequiresDialect(H2Dialect.class)
	void testDomainParsing(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final String sqlQuery = "select id, name from {h-domain}the_table";

		final String full = processSqlString( sqlQuery, "my_catalog", "my_schema", sessionFactory );
		assertThat( full ).endsWith( " my_catalog.my_schema.the_table" );

		final String catalogOnly = processSqlString( sqlQuery, "my_catalog", null, sessionFactory );
		assertThat( catalogOnly ).endsWith( " my_catalog.the_table" );

		final String schemaOnly = processSqlString( sqlQuery, null, "my_schema", sessionFactory );
		assertThat( schemaOnly ).endsWith( " my_schema.the_table" );

		final String none = processSqlString( sqlQuery, null, null, sessionFactory );
		assertThat( none ).endsWith( " the_table" );
	}

	@Test
	@DomainModel
	@SessionFactory
	@RequiresDialect(H2Dialect.class)
	void testCatalogParsing(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final String sqlQuery = "select id, name from {h-catalog}the_table";

		final String full = processSqlString( sqlQuery, "my_catalog", "my_schema", sessionFactory );
		assertThat( full ).endsWith( " my_catalog.the_table" );

		final String catalogOnly = processSqlString( sqlQuery, "my_catalog", null, sessionFactory );
		assertThat( catalogOnly ).endsWith( " my_catalog.the_table" );

		final String schemaOnly = processSqlString( sqlQuery, null, "my_schema", sessionFactory );
		assertThat( schemaOnly ).endsWith( " the_table" );

		final String none = processSqlString( sqlQuery, null, null, sessionFactory );
		assertThat( none ).endsWith( " the_table" );
	}

	@Test
	@DomainModel
	@SessionFactory
	@RequiresDialect(H2Dialect.class)
	void testSchemaParsing(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final String sqlQuery = "select id, name from {h-schema}the_table";

		final String full = processSqlString( sqlQuery, "my_catalog", "my_schema", sessionFactory );
		assertThat( full ).endsWith( " my_schema.the_table" );

		final String catalogOnly = processSqlString( sqlQuery, "my_catalog", null, sessionFactory );
		assertThat( catalogOnly ).endsWith( " the_table" );

		final String schemaOnly = processSqlString( sqlQuery, null, "my_schema", sessionFactory );
		assertThat( schemaOnly ).endsWith( " my_schema.the_table" );

		final String none = processSqlString( sqlQuery, null, null, sessionFactory );
		assertThat( none ).endsWith( " the_table" );
	}

	private static String processSqlString(
			String sqlQuery,
			String catalogName,
			String schemaName,
			SessionFactoryImplementor sessionFactory) {
		// Use a custom SqlStringGenerationContext to integrate the catalog and schema
		final ExplicitSqlStringGenerationContext stringGenerationContext
				= new ExplicitSqlStringGenerationContext( catalogName, schemaName, sessionFactory );
		return new SQLQueryParser( sqlQuery, null, stringGenerationContext ).process();
	}

}
