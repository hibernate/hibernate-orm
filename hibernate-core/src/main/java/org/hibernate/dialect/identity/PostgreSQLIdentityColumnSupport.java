/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import org.hibernate.MappingException;

import java.sql.Types;

/**
 * @author Andrea Boriero
 */
public class PostgreSQLIdentityColumnSupport extends IdentityColumnSupportImpl {
	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select currval('" + table + '_' + column + "_seq')";
	}

	@Override
	public String getIdentityColumnString(int type) {
		switch (type) {
			case Types.BIGINT:
				return "bigserial not null";
			case Types.INTEGER:
				return "serial not null";
			default:
				throw new MappingException("illegal identity column type");
		}
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}
}
