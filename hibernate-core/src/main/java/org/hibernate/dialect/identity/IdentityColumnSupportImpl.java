/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
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
