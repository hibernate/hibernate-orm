/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;

/**
 * @author Andrea Boriero
 */
public class CUBRIDIdentityColumnSupport extends IdentityColumnSupportBase {

	public static final CUBRIDIdentityColumnSupport INSTANCE = new CUBRIDIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityInsertString() {
		return "NULL";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_insert_id()";
	}

	@Override
	public String getIdentityColumnString(int type) {
		//starts with 1, implicitly
		return "not null auto_increment";
	}
}
