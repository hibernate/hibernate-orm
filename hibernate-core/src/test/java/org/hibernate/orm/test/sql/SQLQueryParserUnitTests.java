/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
	void testJDBCEscapeSyntaxParsing(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final String sqlQuery = "select id, name from {h-domain}the_table where date = {d '2025-06-18'}";

		final String full = processSqlString( sqlQuery, "my_catalog", "my_schema", sessionFactory );
		assertThat( full ).contains( "{d '2025-06-18'}" );

		final String catalogOnly = processSqlString( sqlQuery, "my_catalog", null, sessionFactory );
		assertThat( catalogOnly ).contains( "{d '2025-06-18'}" );

		final String schemaOnly = processSqlString( sqlQuery, null, "my_schema", sessionFactory );
		assertThat( schemaOnly ).contains( "{d '2025-06-18'}" );

		final String none = processSqlString( sqlQuery, null, null, sessionFactory );
		assertThat( none ).contains( "{d '2025-06-18'}" );
	}

	private static String processSqlString(
			String sqlQuery,
			String catalogName,
			String schemaName,
			SessionFactoryImplementor sessionFactory) {
		return new SQLQueryParser( sqlQuery, null, sessionFactory ).process();
	}
}
