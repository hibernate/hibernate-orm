/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;


import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * Spatial Dialect for Oracle10g databases.
 *
 * @author Karel Maesen
 *
 * @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class OracleSpatial10gDialect extends OracleDialect implements SpatialDialect {

	public OracleSpatial10gDialect() {
		super( DatabaseVersion.make( 10 ) );
	}

}
