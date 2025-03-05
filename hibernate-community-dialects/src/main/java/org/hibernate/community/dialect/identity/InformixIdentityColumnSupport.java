/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import java.sql.Types;

import org.hibernate.MappingException;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * @author Andrea Boriero
 */
public class InformixIdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final InformixIdentityColumnSupport INSTANCE = new InformixIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type)
			throws MappingException {
		switch (type) {
			case Types.BIGINT:
				return "select dbinfo('serial8') from informix.systables where tabid=1";
			case Types.INTEGER:
				return "select dbinfo('sqlca.sqlerrd1') from informix.systables where tabid=1";
			default:
				throw new MappingException("illegal identity column type");
		}
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		switch (type) {
			case Types.BIGINT:
				return "serial8 not null";
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
