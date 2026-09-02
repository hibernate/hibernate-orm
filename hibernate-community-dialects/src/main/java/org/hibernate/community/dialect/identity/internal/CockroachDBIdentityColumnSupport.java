/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

import java.sql.Types;

import org.hibernate.MappingException;
import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;

/// Community CockroachDB identity-column profile.
///
/// @author Steve Ebersole
public final class CockroachDBIdentityColumnSupport extends IdentityColumnSupportBase {
	public static final CockroachDBIdentityColumnSupport INSTANCE = new CockroachDBIdentityColumnSupport();

	private CockroachDBIdentityColumnSupport() {
	}

	@Override
	public boolean supportsIdentityColumns() {
		return false;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int jdbcTypeCode) {
		return "select 1";
	}

	@Override
	public String getIdentityColumnString(int jdbcTypeCode) {
		return switch ( jdbcTypeCode ) {
			case Types.TINYINT, Types.SMALLINT -> "serial2 not null";
			case Types.INTEGER -> "serial4 not null";
			case Types.BIGINT -> "serial8 not null";
			default -> throw new MappingException( "illegal identity column type" );
		};
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}
}
