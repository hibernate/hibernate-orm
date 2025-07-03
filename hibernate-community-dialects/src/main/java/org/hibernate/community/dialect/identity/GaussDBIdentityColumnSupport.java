/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

import static org.hibernate.internal.util.StringHelper.unquote;

/**
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLIdentityColumnSupport.
 */
public class GaussDBIdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final GaussDBIdentityColumnSupport INSTANCE = new GaussDBIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select currval('" + unquote(table) + '_' + unquote(column) + "_seq')";
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "bigserial";
	}
}
