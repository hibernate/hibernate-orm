/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import java.sql.Types;

import org.hibernate.MappingException;

/**
 * @author Andrea Boriero
 */
public class InformixIdentityColumnSupport extends IdentityColumnSupportImpl {
	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type)
			throws MappingException {
		return type == Types.BIGINT
				? "select dbinfo('serial8') from informix.systables where tabid=1"
				: "select dbinfo('sqlca.sqlerrd1') from informix.systables where tabid=1";
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		return type == Types.BIGINT ?
				"serial8 not null" :
				"serial not null";
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}
}
