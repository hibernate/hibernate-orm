/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * @author Andrea Boriero
 */
public class Ingres9IdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final Ingres9IdentityColumnSupport INSTANCE = new Ingres9IdentityColumnSupport();

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_identity()";
	}
}
