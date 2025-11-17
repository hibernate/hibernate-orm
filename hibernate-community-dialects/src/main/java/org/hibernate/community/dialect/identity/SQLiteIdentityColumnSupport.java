/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * See https://sqlite.org/autoinc.html and
 * https://github.com/nhibernate/nhibernate-core/blob/master/src/NHibernate/Dialect/SQLiteDialect.cs for details.
 *
 * @author Andrea Boriero
 */
public class SQLiteIdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final SQLiteIdentityColumnSupport INSTANCE = new SQLiteIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "integer";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_insert_rowid()";
	}
}
