/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/// Minimal Dialect fixture for migration-inventory tests.
///
/// @author Steve Ebersole
public class Dialect {
	protected static final String POLICY = "policy";

	public String translate(String value) {
		return render( value );
	}

	protected String render(String value) {
		return value;
	}

	public final String familyName() {
		return "fixture";
	}

	public boolean supportsUsefulFeature() {
		return true;
	}
}
