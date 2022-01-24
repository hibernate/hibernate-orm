/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * A Spatial Dialect for Oracle 10g/11g that uses the "native" SDO spatial operators.
 *
 *  @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class OracleSpatialSDO10gDialect extends Oracle10gDialect
		implements SpatialDialect {
}
