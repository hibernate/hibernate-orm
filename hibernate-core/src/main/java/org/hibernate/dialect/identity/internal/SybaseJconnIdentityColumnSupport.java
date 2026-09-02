/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity.internal;

import org.hibernate.dialect.identity.spi.IdentityValueRetrieval;

public class SybaseJconnIdentityColumnSupport extends AbstractTransactSQLIdentityColumnSupport {
	public static final SybaseJconnIdentityColumnSupport INSTANCE = new SybaseJconnIdentityColumnSupport();

	@Override
	public IdentityValueRetrieval getIdentityValueRetrieval() {
		return IdentityValueRetrieval.APPENDED_SELECT;
	}
}
