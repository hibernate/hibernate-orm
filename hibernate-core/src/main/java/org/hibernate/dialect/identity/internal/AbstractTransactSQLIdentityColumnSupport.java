/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity.internal;

import org.hibernate.MappingException;
import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;

/**
 * @author Andrea Boriero
 */
public class AbstractTransactSQLIdentityColumnSupport extends IdentityColumnSupportBase {

	public static final AbstractTransactSQLIdentityColumnSupport INSTANCE = new AbstractTransactSQLIdentityColumnSupport();
	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		//starts with 1, implicitly
		return "identity not null";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) throws MappingException {
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
