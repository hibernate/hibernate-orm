/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

/// Community SQL Server identity-column profile.
///
/// @author Steve Ebersole
public final class SQLServerIdentityColumnSupport extends TransactSQLIdentityColumnSupport {
	public static final SQLServerIdentityColumnSupport INSTANCE = new SQLServerIdentityColumnSupport();

	private SQLServerIdentityColumnSupport() {
	}

	@Override
	public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return insertString + " select scope_identity()";
	}
}
