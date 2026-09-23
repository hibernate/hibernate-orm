/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Explicit projection at boundaries which still supply finalized identifier text.
/// Does not invoke physical naming or quote normalization.
///
/// @author Steve Ebersole
public final class ColumnNameHelper {
	private ColumnNameHelper() {}

	public static PhysicalName physicalName(String spelling, Database database) {
		final var identifier = Identifier.toIdentifier( spelling );
		return physicalName( identifier, database );
	}

	public static PhysicalName physicalName(Identifier identifier, Database database) {
		return identifier == null ? null : database.getJdbcEnvironment().getIdentifierHelper()
				.getPhysicalNameFactory().create( identifier.getText(), identifier.isQuoted() );
	}

	public static Identifier identifier(org.hibernate.mapping.Column column) {
		return PhysicalNamingStrategyHelper.physicalIdentifier( column.getPhysicalName() );
	}
}
