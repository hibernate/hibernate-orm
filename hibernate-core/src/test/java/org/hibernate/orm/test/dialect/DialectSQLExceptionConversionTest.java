/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dialect;

import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class DialectSQLExceptionConversionTest {

	private final Dialect dialect = DialectContext.getDialect();

	@Test
	@JiraKey(value = "HHH-15592")
	public void testExceptionConversionDoesntNPE() {
		final SQLExceptionConversionDelegate conversionDelegate = dialect.buildSQLExceptionConversionDelegate();
		Assumptions.assumeTrue( conversionDelegate != null );
		conversionDelegate.convert(
				new SQLException(),
				"test",
				"test"
		);
	}
}
