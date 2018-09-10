/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12939")
public abstract class AbstractAlterTableQuoteSchemaTest extends BaseCoreFunctionalTestCase {

	private Dialect dialect = Dialect.getDialect();

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
