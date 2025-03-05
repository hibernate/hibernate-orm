/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity;

import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.id.insert.SybaseJConnGetGeneratedKeysDelegate;
import org.hibernate.persister.entity.EntityPersister;

public class SybaseJconnIdentityColumnSupport extends AbstractTransactSQLIdentityColumnSupport {
	public static final SybaseJconnIdentityColumnSupport INSTANCE = new SybaseJconnIdentityColumnSupport();

	@Override
	public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(EntityPersister persister) {
		return new SybaseJConnGetGeneratedKeysDelegate( persister );
	}
}
