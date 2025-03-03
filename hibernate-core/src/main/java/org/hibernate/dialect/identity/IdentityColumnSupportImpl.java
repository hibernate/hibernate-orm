/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity;

import org.hibernate.MappingException;
import org.hibernate.generator.EventType;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Andrea Boriero
 */
public class IdentityColumnSupportImpl implements IdentityColumnSupport {

	public static final IdentityColumnSupportImpl INSTANCE = new IdentityColumnSupportImpl();

	@Override
	public boolean supportsIdentityColumns() {
		return false;
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return false;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return true;
	}

	@Override
	public String appendIdentitySelectToInsert(String insertString) {
		return insertString;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) throws MappingException {
		throw new MappingException( getClass().getName() + " does not support identity key generation" );
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		throw new MappingException( getClass().getName() + " does not support identity key generation" );
	}

	@Override
	public String getIdentityInsertString() {
		return null;
	}

	@Override
	public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(EntityPersister persister) {
		return new GetGeneratedKeysDelegate( persister, true, EventType.INSERT );
	}
}
