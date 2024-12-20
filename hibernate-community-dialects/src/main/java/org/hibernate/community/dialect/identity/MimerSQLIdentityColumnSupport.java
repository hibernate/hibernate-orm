/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * @author Andrea Boriero
 */
public class MimerSQLIdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final MimerSQLIdentityColumnSupport INSTANCE = new MimerSQLIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return false;
	}
}
