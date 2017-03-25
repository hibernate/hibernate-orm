/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.mysql;

import org.hibernate.dialect.InnoDBStorageEngine;
import org.hibernate.dialect.MySQLStorageEngine;

/**
 * A Dialect for MySQL 5 using InnoDB engine, with support for its spatial features
 *
 * @author Karel Maesen, Geovise BVBA
 * @deprecated Use "hibernate.dialect.storage_engine=innodb" environment variable or JVM system property instead.
 */
@Deprecated
public class MySQL5InnoDBSpatialDialect extends MySQL5SpatialDialect {

	@Override
	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return InnoDBStorageEngine.INSTANCE;
	}
}
