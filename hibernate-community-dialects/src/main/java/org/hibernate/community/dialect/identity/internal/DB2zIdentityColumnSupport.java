/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

/// Community DB2 for z/OS identity-column profile.
///
/// @author Steve Ebersole
public final class DB2zIdentityColumnSupport extends DB2IdentityColumnSupport {
	public static final DB2zIdentityColumnSupport INSTANCE = new DB2zIdentityColumnSupport();

	private DB2zIdentityColumnSupport() {
	}

	@Override
	public String getIdentitySelectString(String table, String column, int jdbcTypeCode) {
		return "select identity_val_local() from sysibm.sysdummy1";
	}
}
