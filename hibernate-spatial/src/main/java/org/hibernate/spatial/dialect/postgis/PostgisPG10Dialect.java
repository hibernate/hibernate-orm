/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;


import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

@Deprecated
public class PostgisPG10Dialect  extends PostgreSQLDialect {

	public PostgisPG10Dialect(DialectResolutionInfo resolutionInfo) {
		super( resolutionInfo );
	}

	public PostgisPG10Dialect() {
		super( DatabaseVersion.make( 10 ) );
	}

}
