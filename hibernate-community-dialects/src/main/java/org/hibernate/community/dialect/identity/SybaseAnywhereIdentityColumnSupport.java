/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.AbstractTransactSQLIdentityColumnSupport;

/**
 * @author Andrea Boriero
 */
public class SybaseAnywhereIdentityColumnSupport extends AbstractTransactSQLIdentityColumnSupport {

	public static final SybaseAnywhereIdentityColumnSupport INSTANCE = new SybaseAnywhereIdentityColumnSupport();

	@Override
	public boolean supportsInsertSelectIdentity() {
		return false;
	}
}
