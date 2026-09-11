/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import java.sql.SQLException;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.mapping.Formula;
import org.hibernate.sql.Template;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-20840")
class OracleSysdateTest {

	@Test
	void testSysdateFormulaWithoutJdbcMetadata() {
		final var dialect = new OracleDialect();
		final var types = new TypeConfiguration();
		assertEquals( "sysdate", new Formula( "sysdate" ).getTemplate( dialect, types ) );
		assertEquals( "SYSDATE", new Formula( "SYSDATE" ).getTemplate( dialect, types ) );
		assertEquals( "sysdate - {@}.created_at",
				new Formula( "sysdate - created_at" ).getTemplate( dialect, types ) );
		assertEquals( "p.created_at < sysdate",
				Template.renderWhereStringTemplate( "created_at < sysdate", "p", dialect, types ) );
	}

	@Test
	void testQuotedAndQualifiedColumns() {
		final var dialect = new OracleDialect();
		final var types = new TypeConfiguration();
		assertEquals( "{@}.\"SYSDATE\"",
				new Formula( "\"SYSDATE\"" ).getTemplate( dialect, types ) );
		assertEquals( "{@}.\"SYSDATE\"",
				new Formula( "`SYSDATE`" ).getTemplate( dialect, types ) );
		assertEquals( "p.sysdate", new Formula( "p.sysdate" ).getTemplate( dialect, types ) );
		assertEquals( "upper({@}.name)", new Formula( "upper(name)" ).getTemplate( dialect, types ) );
	}

	@Test
	void testSysdateColumnOnH2() {
		assertEquals( "{@}.sysdate",
				new Formula( "sysdate" ).getTemplate( new H2Dialect(), new TypeConfiguration() ) );
	}

	@Test
	void testKeywordAutoQuoting() throws SQLException {
		final var builder = IdentifierHelperBuilder.from( null );
		builder.setAutoQuoteKeywords( true );
		final var helper = new OracleDialect().buildIdentifierHelper( builder, null );
		assertTrue( helper.toIdentifier( "sysdate" ).isQuoted() );
		assertTrue( helper.toIdentifier( "SYSDATE" ).isQuoted() );
		assertFalse( helper.toIdentifier( "created_at" ).isQuoted() );
	}
}
