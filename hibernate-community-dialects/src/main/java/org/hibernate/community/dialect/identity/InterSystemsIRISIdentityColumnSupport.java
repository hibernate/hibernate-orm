/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.MappingException;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

public class InterSystemsIRISIdentityColumnSupport extends IdentityColumnSupportImpl {

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		return "IDENTITY";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "SELECT LAST_IDENTITY() FROM %TSQL_sys.snf";
	}

	@Override
	public String getIdentityInsertString() {
		return null;
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return false;
	}

}
