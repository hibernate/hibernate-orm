/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.MappingException;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * @author Andrea Boriero
 */
public class CacheIdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final CacheIdentityColumnSupport INSTANCE = new CacheIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		// Whether this dialect has an Identity clause added to the data type or a completely separate identity
		// data type
		return true;
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		// The keyword used to specify an identity column, if identity column key generation is supported.
		return "identity";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "SELECT LAST_IDENTITY() FROM %TSQL_sys.snf";
	}
}
