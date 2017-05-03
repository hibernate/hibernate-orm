/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

public class SQLiteDialectIdentityColumnSupport extends IdentityColumnSupportImpl {
	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

  /*
	public boolean supportsInsertSelectIdentity() {
    return true; // As specified in NHibernate dialect
  }
  */

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		// As specified in NHibernate dialect
		// FIXME true
		return false;
	}

  /*
	public String appendIdentitySelectToInsert(String insertString) {
    return new StringBuffer(insertString.length()+30). // As specified in NHibernate dialect
      append(insertString).
      append("; ").append(getIdentitySelectString()).
      toString();
  }
  */

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_insert_rowid()";
	}

	@Override
	public String getIdentityColumnString(int type) {
		// return "integer primary key autoincrement";
		// FIXME "autoincrement"
		return "integer";
	}
}
