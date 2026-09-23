/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.PhysicalTable;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

/// Creates standalone mapping fixtures with an explicitly supplied comparison policy.
///
/// @author Steve Ebersole
public final class MappingTableHelper {
	private MappingTableHelper() {
	}

	public static PhysicalName columnName(String spelling, PhysicalName.Factory factory) {
		final var identifier = Identifier.toIdentifier( spelling );
		return factory.create( identifier.getText(), identifier.isQuoted() );
	}

	public static PhysicalTable table(String contributor, String name, PhysicalName.Factory factory) {
		final var identifier = Identifier.toIdentifier( name );
		return new PhysicalTable( contributor,
				new QualifiedPhysicalName( null, null, factory.create( identifier.getText(), identifier.isQuoted() ) ), false );
	}
}
