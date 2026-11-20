/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Verifies that {@link PostgreSQLLegacyDialect} enables {@code Types.NULL} binding for untyped nulls
 * (HHH-21100 / HHH-18581), so the driver avoids a {@code getParameterMetaData()} round-trip per null.
 */
public class PostgreSQLLegacyDialectNullBindingTest {

	@Test
	public void postgresqlLegacyDialectSupportsNullTypeBinding() {
		assertTrue( new PostgreSQLLegacyDialect().supportsBindingNullSqlTypeForSetNull() );
		assertTrue( new PostgreSQLLegacyDialect( DatabaseVersion.make( 8, 0 ) ).supportsBindingNullSqlTypeForSetNull() );
		assertTrue( new PostgreSQLLegacyDialect( DatabaseVersion.make( 9, 4 ) ).supportsBindingNullSqlTypeForSetNull() );
		assertTrue( new PostgreSQLLegacyDialect( DatabaseVersion.make( 12 ) ).supportsBindingNullSqlTypeForSetNull() );
		assertTrue( new PostgresPlusLegacyDialect().supportsBindingNullSqlTypeForSetNull() );
	}
}
