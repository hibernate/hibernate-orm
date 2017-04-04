/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

/**
* In SQLite, any "integer primary key" is automatically considered an IDENTITY column.
* A keyword "autoincrement" can be added to avoid reuse indexes of deleted rows,
* however this is not strictly required to be an IDENTITY column.
* We get this purpose assuming a fake getIdentityColumnString() == "integer".
*
* @see https://sqlite.org/autoinc.html and 
* https://github.com/nhibernate/nhibernate-core/blob/master/src/NHibernate/Dialect/SQLiteDialect.cs
*/
/*
TODO check if and how SQlite supportsInsertSelectIdentity() 
*/
public class SQLiteDialectIdentityColumnSupport extends IdentityColumnSupportImpl {
	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_insert_rowid()";
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "integer";
	}
}
