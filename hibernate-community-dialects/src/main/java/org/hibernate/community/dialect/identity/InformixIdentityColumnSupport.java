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
		return "select dbinfo('" + switch ( type ) {
			case Types.BIGINT -> "bigserial";
			case Types.INTEGER -> "sqlca.sqlerrd1";
			default -> throw new MappingException( "illegal identity column type" );
		} + "') from informix.systables where tabid=1";
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		return switch ( type ) {
			case Types.BIGINT -> "bigserial";
			case Types.INTEGER -> "serial";
			default -> throw new MappingException( "illegal identity column type" );
		} + " not null";
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}
}
