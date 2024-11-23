/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import java.sql.Types;

public class CockroachDB1920IdentityColumnSupport extends IdentityColumnSupportImpl {
	@Override
	public boolean supportsIdentityColumns() {
		// Full support requires setting the sql.defaults.serial_normalization=sql_sequence in CockroachDB.
		return false;
	}

	@Override
	// CockroachDB does not create a sequence for id columns
	public String getIdentitySelectString(String table, String column, int type) {
		return "select 1";
	}

	@Override
	public String getIdentityColumnString(int type) {
		return type == Types.SMALLINT ?
				"serial4 not null" :
				"serial8 not null";
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}
}
