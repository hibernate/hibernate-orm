/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Extends the {@code PostgreSQL93Dialect} to add support for the Postgis spatial types, functions and operators .
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 *
 * @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class PostgisPG93Dialect extends PostgreSQLDialect {
	public PostgisPG93Dialect(DialectResolutionInfo info) {
		super( info );
	}

	public PostgisPG93Dialect() {
		super( DatabaseVersion.make( 9, 3 ) );
	}
}
