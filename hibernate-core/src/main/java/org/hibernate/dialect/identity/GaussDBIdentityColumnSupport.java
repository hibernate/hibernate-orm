/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identity;


/**
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLIdentityColumnSupport.
 */
public class GaussDBIdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final GaussDBIdentityColumnSupport INSTANCE = new GaussDBIdentityColumnSupport();

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "";
	}

	@Override
	public String getIdentityColumnString(int type) {
		return  "";
	}
}
