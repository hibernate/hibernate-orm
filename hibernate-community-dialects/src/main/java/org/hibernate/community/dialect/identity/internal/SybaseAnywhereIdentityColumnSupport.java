/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;


/**
 * @author Andrea Boriero
 */
public class SybaseAnywhereIdentityColumnSupport extends TransactSQLIdentityColumnSupport {

	public static final SybaseAnywhereIdentityColumnSupport INSTANCE = new SybaseAnywhereIdentityColumnSupport();

	@Override
	public boolean supportsInsertSelectIdentity() {
		return false;
	}
}
