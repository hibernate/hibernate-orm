/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * A {@linkplain Dialect SQL dialect} for Azure SQL Server.
 */
public class AzureSQLServerDialect extends SQLServerDialect {

	public AzureSQLServerDialect() {
		// Azure SQL Server always is the latest version, so default to a high number
		super( DatabaseVersion.make( Integer.MAX_VALUE ) );
	}

	public AzureSQLServerDialect(DialectResolutionInfo info) {
		this();
		registerKeywords( info );
	}
}
