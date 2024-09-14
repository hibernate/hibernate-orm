/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.cfg.Environment;
import org.hibernate.community.dialect.OracleLegacyDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
public class OracleDialectsTest {

	@Test
	@JiraKey("HHH-9990")
	public void testDefaultBatchVersionDataProperty() {
		Dialect oracleDialect = new OracleDialect();
		assertEquals( "true", oracleDialect.getDefaultProperties().getProperty( Environment.BATCH_VERSIONED_DATA ) );

		Dialect oracle8iDialect = new OracleLegacyDialect( DatabaseVersion.make( 8 ) );
		assertEquals( "false", oracle8iDialect.getDefaultProperties().getProperty( Environment.BATCH_VERSIONED_DATA ) );

		Dialect oracle10gDialect = new OracleLegacyDialect( DatabaseVersion.make( 10 ) );
		assertEquals( "false", oracle10gDialect.getDefaultProperties().getProperty( Environment.BATCH_VERSIONED_DATA ) );

		Dialect oracle9iDialect = new OracleLegacyDialect( DatabaseVersion.make( 9 ) );
		assertEquals( "false", oracle9iDialect.getDefaultProperties().getProperty( Environment.BATCH_VERSIONED_DATA ) );

		Dialect oracle12cDialect = new OracleLegacyDialect( DatabaseVersion.make( 12 ) );
		assertEquals( "true", oracle12cDialect.getDefaultProperties().getProperty( Environment.BATCH_VERSIONED_DATA ) );
	}
}
