/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * *  Extends the {@code PostgreSQL9Dialect} to add support for the Postgis spatial types, functions and operators .
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 *
 * @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class PostgisPG9Dialect extends PostgreSQLDialect {
	public PostgisPG9Dialect(DialectResolutionInfo info) {
	}

	public PostgisPG9Dialect() {
		super();
	}

	public PostgisPG9Dialect(int version) {
		super( version );
	}
}
