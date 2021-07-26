/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import org.hibernate.dialect.PostgreSQL92Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.spatial.SpatialDialect;

/**
 * Extends the {@code PostgreSQL92Dialect} to add support for the Postgis spatial types, functions and operators .
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 01/11/16.
 *
 * @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class PostgisPG92Dialect extends PostgreSQLDialect {
	public PostgisPG92Dialect(DialectResolutionInfo info) {
		super( info );
	}

	public PostgisPG92Dialect() {
		super();
	}

	public PostgisPG92Dialect(int version) {
		super( version );
	}
}
