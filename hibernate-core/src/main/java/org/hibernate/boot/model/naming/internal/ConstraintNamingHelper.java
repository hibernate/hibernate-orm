/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.internal;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.relational.naming.spi.LogicalName;

/// Resolves explicit or implicit constraint/index names and invokes their physical policy.
///
/// @author Steve Ebersole
public final class ConstraintNamingHelper {
	public enum Kind { PRIMARY_KEY, FOREIGN_KEY, UNIQUE_KEY, INDEX }

	private ConstraintNamingHelper() {
	}

	public static String resolve(String explicitName, Supplier<Identifier> implicitName,
			Kind kind, MetadataBuildingContext context) {
		final boolean explicit = explicitName != null && !explicitName.isEmpty();
		final Identifier identifier = explicit
				? Identifier.toIdentifier( explicitName, false, false, true ) : implicitName.get();
		if ( identifier == null ) {
			throw new MappingException( "Implicit naming strategy returned null for " + kind );
		}
		final var logical = new LogicalName( identifier.getText(), identifier.isQuoted(), explicit );
		return resolveLogical( logical, kind, context );
	}

	public static String resolveLogical(LogicalName logical, Kind kind, MetadataBuildingContext context) {
		if ( logical == null ) {
			throw new MappingException( "Implicit naming strategy returned null for " + kind );
		}
		final var database = context.getMetadataCollector().getDatabase();
		final var strategy = context.getBuildingPlan().getPhysicalNamingStrategy();
		final var physical = PhysicalNamingStrategyHelper.resolve( logical, database.getJdbcEnvironment(),
				switch ( kind ) {
					case PRIMARY_KEY -> strategy::toPhysicalPrimaryKeyName;
					case FOREIGN_KEY -> strategy::toPhysicalForeignKeyName;
					case UNIQUE_KEY -> strategy::toPhysicalUniqueKeyName;
					case INDEX -> strategy::toPhysicalIndexName;
				}, kind.name(), false );
		return PhysicalNamingStrategyHelper.physicalIdentifier( physical ).render( database.getDialect() );
	}
}
