/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Extends the {@code PostgreSQL95Dialect} to add support for the Postgis spatial types, functions and operators .
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 *
 * @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class PostgisPG95Dialect extends PostgreSQLDialect {
	public PostgisPG95Dialect(DialectResolutionInfo info) {
		super( info );
	}

	public PostgisPG95Dialect() {
		super();
	}
}
