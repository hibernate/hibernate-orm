/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;


/**
 * Represents the MyISAM storage engine.
 *
 * @author Vlad Mihalcea
 */
public class MyISAMStorageEngine implements MySQLStorageEngine {

	public static final MySQLStorageEngine INSTANCE = new MyISAMStorageEngine();

	@Override
	public boolean supportsOnDeleteAction(org.hibernate.annotations.OnDeleteAction action) {
		return action == org.hibernate.annotations.OnDeleteAction.NO_ACTION;
	}

	@Override
	public String getTableTypeString(String engineKeyword) {
		return String.format( " %s=MyISAM", engineKeyword );
	}

	@Override
	public boolean requiresSelfReferentialForeignKeyNullification() {
		return false;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}
}
