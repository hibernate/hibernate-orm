/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.PostInsertIdentityPersister;

/**
 * @author Andrea Boriero
 */
public class IdentityColumnSupportImpl implements IdentityColumnSupport {
	private final Dialect dialect;

	public IdentityColumnSupportImpl(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public boolean supportsIdentityColumns() {
		return dialect.supportsIdentityColumns();
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return dialect.supportsInsertSelectIdentity();
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return dialect.hasDataTypeInIdentityColumn();
	}

	@Override
	public String appendIdentitySelectToInsert(String insertString) {
		return dialect.appendIdentitySelectToInsert( insertString );
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) throws MappingException {
		return dialect.getIdentitySelectString( table, column, type );
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		return dialect.getIdentityColumnString( type );
	}

	@Override
	public String getIdentityInsertString() {
		return dialect.getIdentityInsertString();
	}

	@Override
	public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect) {
		return new GetGeneratedKeysDelegate( persister, dialect );
	}
}
