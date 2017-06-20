/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Represents the InnoDB storage engine.
 *
 * @author Vlad Mihalcea
 */
public class InnoDBStorageEngine implements MySQLStorageEngine{

	public static final MySQLStorageEngine INSTANCE = new InnoDBStorageEngine();

	@Override
	public boolean supportsCascadeDelete() {
		return true;
	}

	@Override
	public String getTableTypeString(String engineKeyword) {
		return String.format( " %s=InnoDB", engineKeyword );
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}
}
