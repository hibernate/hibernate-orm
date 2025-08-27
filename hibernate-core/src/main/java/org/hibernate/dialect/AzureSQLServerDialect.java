/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
