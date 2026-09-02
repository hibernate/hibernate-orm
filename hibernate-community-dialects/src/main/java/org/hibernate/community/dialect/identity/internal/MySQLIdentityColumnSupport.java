/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;

/// Community MySQL identity-column profile.
///
/// @author Steve Ebersole
public class MySQLIdentityColumnSupport extends IdentityColumnSupportBase {
	public static final MySQLIdentityColumnSupport INSTANCE = new MySQLIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int jdbcTypeCode) {
		return "select last_insert_id()";
	}

	@Override
	public String getIdentityColumnString(int jdbcTypeCode) {
		return "not null auto_increment";
	}
}
