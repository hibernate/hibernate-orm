/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * See https://sqlite.org/autoinc.html and
 * https://github.com/nhibernate/nhibernate-core/blob/master/src/NHibernate/Dialect/SQLiteDialect.cs for details.
 *
 * @author Andrea Boriero
 */
public class SQLiteIdentityColumnSupport extends IdentityColumnSupportImpl {

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "integer";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_insert_rowid()";
	}
}
