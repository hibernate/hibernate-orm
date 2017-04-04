/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.unit.lockhint;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;

/**
 * @author Steve Ebersole
 */
public class SQLServerLockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SQLServerDialect();

	protected String getLockHintUsed() {
		return "with (updlock, rowlock)";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}
}
