/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.sqlserver;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/09/2018.
 * @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class SqlServerSpatialDialect extends SQLServerDialect implements SpatialDialect {

}
