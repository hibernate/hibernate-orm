/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/** A Dialect for MySQL 5 using InnoDB engine
 *
 * @author Gavin King,
 * @author Scott Marlow
 * @deprecated Use "hibernate.dialect.storage_engine=innodb" environment variable or JVM system property instead.
 */
@Deprecated
public class MySQL5InnoDBDialect extends MySQL5Dialect {

	@Override
	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return InnoDBStorageEngine.INSTANCE;
	}
}
