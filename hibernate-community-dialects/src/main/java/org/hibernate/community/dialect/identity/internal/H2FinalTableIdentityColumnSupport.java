/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity.internal;

/// Community H2 2 identity-column profile.
///
/// @author Steve Ebersole
public final class H2FinalTableIdentityColumnSupport extends H2IdentityColumnSupport {
	public static final H2FinalTableIdentityColumnSupport INSTANCE = new H2FinalTableIdentityColumnSupport();

	private H2FinalTableIdentityColumnSupport() {
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	@Override
	public String appendIdentitySelectToInsert(String identityColumnName, String insertString) {
		return "select " + identityColumnName + " from final table ( " + insertString + " )";
	}
}
