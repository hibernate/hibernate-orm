/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
