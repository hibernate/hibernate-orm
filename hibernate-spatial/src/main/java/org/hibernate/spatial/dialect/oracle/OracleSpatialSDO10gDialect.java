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
 * A Spatial Dialect for Oracle 10g/11g that uses the "native" SDO spatial operators.
 *
 *  @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class OracleSpatialSDO10gDialect extends OracleDialect implements SpatialDialect {

	public OracleSpatialSDO10gDialect() {
		super( DatabaseVersion.make( 10 ) );
	}
}
