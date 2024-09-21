/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
