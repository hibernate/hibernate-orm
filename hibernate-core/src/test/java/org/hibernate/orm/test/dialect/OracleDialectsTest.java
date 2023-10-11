/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
