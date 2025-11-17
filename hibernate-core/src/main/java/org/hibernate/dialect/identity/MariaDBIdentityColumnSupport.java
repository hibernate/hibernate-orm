/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity;

/**
 * @author Marco Belladelli
 */
public class MariaDBIdentityColumnSupport extends MySQLIdentityColumnSupport {
	public static final MariaDBIdentityColumnSupport INSTANCE = new MariaDBIdentityColumnSupport();

	@Override
	public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return insertString + " returning " + identityColumnName;
	}
}
