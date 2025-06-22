/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * Represents the MyISAM storage engine.
 *
 * @author Vlad Mihalcea
 */
public class MyISAMStorageEngine implements MySQLStorageEngine {

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
