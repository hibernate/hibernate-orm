/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * A Dialect for MySQL using the MyISAM engine
 *
 * @author Gavin King
 */
public class MySQLMyISAMDialect extends MySQLDialect {
	@Override
	public String getTableTypeString() {
		return " type=MyISAM";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}
}
