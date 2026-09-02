/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;

/**
 * @author Andrea Boriero
 */
public class Ingres9IdentityColumnSupport extends IdentityColumnSupportBase {

	public static final Ingres9IdentityColumnSupport INSTANCE = new Ingres9IdentityColumnSupport();

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_identity()";
	}
}
