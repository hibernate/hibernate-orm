/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.mysql;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 10/9/13
 */

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * Extends the MySQL5Dialect by including support for the spatial operators.
 * <p>
 * This <code>SpatialDialect</code> uses the ST_* spatial operators that operate on exact geometries which have been
 * added in MySQL version 5.6.1. Previous versions of MySQL only supported operators that operated on Minimum Bounding
 * Rectangles (MBR's). This dialect my therefore produce different results than the other MySQL spatial dialects.
 *
 * @author Karel Maesen
 * @deprecated Spatial Dialects are no longer needed
 */
@Deprecated
public class MySQL56SpatialDialect extends MySQLDialect implements SpatialDialect {

	public MySQL56SpatialDialect() {
		super( DatabaseVersion.make( 5, 5 ) );
	}
}
