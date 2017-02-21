/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Represents the MyISAM storage engine.
 * 
 * @author Vlad Mihalcea
 */
public class MyISAMStorageEngine implements MySQLStorageEngine{

	public static final MySQLStorageEngine INSTANCE = new MyISAMStorageEngine();

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public String getTableTypeString(String engineKeyword) {
		return String.format( " %s=MyISAM", engineKeyword );
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return false;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}
}
