/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity;

/**
 * @author Andrea Boriero
 */
public class DB2zIdentityColumnSupport extends DB2IdentityColumnSupport {

	public static final DB2zIdentityColumnSupport INSTANCE = new DB2zIdentityColumnSupport();

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select identity_val_local() from sysibm.sysdummy1";
	}
}
