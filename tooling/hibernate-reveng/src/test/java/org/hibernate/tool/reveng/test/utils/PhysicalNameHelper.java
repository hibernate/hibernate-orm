/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.test.utils;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Parses test spellings using an explicitly supplied system factory.
///
/// @author Steve Ebersole
public final class PhysicalNameHelper {
	private PhysicalNameHelper() {}

	public static PhysicalName columnName(String spelling, PhysicalName.Factory factory) {
		final var identifier = Identifier.toIdentifier( spelling );
		return factory.create( identifier.getText(), identifier.isQuoted() );
	}
}
