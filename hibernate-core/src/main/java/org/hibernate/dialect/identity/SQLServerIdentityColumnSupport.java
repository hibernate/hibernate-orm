/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity;

/**
 * @author Andrea Boriero
 */
public class SQLServerIdentityColumnSupport extends AbstractTransactSQLIdentityColumnSupport {

	public static final SQLServerIdentityColumnSupport INSTANCE = new SQLServerIdentityColumnSupport();

	/**
	 * Use {@code insert table(...) values(...) select SCOPE_IDENTITY()}
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + " select scope_identity()";
	}
}
