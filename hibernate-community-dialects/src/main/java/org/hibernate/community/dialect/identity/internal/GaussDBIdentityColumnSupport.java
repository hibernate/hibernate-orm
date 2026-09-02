/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;

/**
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLIdentityColumnSupport.
 */
public class GaussDBIdentityColumnSupport extends IdentityColumnSupportBase {

	public static final GaussDBIdentityColumnSupport INSTANCE = new GaussDBIdentityColumnSupport();

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
		return "select currval('" + unquote(table) + '_' + unquote(column) + "_seq')";
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "bigserial";
	}

	private static String unquote(String name) {
		if ( name == null || name.isEmpty() ) {
			return name;
		}
		final char first = name.charAt( 0 );
		final char last = name.charAt( name.length() - 1 );
		return first == last && ( first == '`' || first == '"' )
				? name.substring( 1, name.length() - 1 )
				: name;
	}
}
