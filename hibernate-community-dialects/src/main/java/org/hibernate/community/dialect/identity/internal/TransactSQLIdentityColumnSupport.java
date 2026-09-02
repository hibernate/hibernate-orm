/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;

/// Community Transact-SQL identity-column profile.
///
/// @author Steve Ebersole
public class TransactSQLIdentityColumnSupport extends IdentityColumnSupportBase {
	public static final TransactSQLIdentityColumnSupport INSTANCE = new TransactSQLIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int jdbcTypeCode) {
		return "identity not null";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int jdbcTypeCode) {
		return "select @@identity";
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	@Override
	public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return insertString + "\nselect @@identity";
	}
}
