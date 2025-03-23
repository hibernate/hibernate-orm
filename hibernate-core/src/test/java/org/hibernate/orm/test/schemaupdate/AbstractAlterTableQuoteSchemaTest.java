/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import org.hibernate.dialect.Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectContext;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12939")
public abstract class AbstractAlterTableQuoteSchemaTest extends BaseSessionFactoryFunctionalTest {

	private final Dialect dialect = DialectContext.getDialect();

	protected String quote(String element) {
		return dialect.quote( "`" + element + "`" );
	}

	protected String quote(String schema, String table) {
		return quote( schema ) + "." + quote( table );
	}

	protected String regexpQuote(String element) {
		return dialect.quote( "`" + element + "`" )
				.replace( "-", "\\-" )
				.replace( "[", "\\[" )
				.replace( "]", "\\]" );
	}

	protected String regexpQuote(String schema, String table) {
		return regexpQuote( schema ) + "\\." + regexpQuote( table );
	}
}
