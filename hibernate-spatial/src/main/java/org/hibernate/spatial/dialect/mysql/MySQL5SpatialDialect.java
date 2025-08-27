/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * A Dialect for MySQL 5 using InnoDB engine, with support for its spatial features
 *
 * @author Karel Maesen, Geovise BVBA
 * @deprecated Spatial Dialects are no longer needed
 */
@Deprecated
public class MySQL5SpatialDialect extends MySQLDialect implements SpatialDialect {

	public MySQL5SpatialDialect() {
		super( DatabaseVersion.make( 5 ) );
	}
}
