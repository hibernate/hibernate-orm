/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

/// Internal translator fixture used to prove bytecode dependency discovery.
///
/// @author Steve Ebersole
public class InternalTranslator {
	public String translate(String value) {
		return value;
	}
}
