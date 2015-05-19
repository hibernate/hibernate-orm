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
 */
public class MySQL5InnoDBDialect extends MySQL5Dialect {
	@Override
	public boolean supportsCascadeDelete() {
		return true;
	}

	@Override
	public String getTableTypeString() {
		return " ENGINE=InnoDB";
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}
}
