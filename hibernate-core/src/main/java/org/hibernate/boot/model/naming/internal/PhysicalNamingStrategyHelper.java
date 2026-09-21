/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.internal;

import java.util.function.BiFunction;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Invokes physical naming and finalizes additive quoting. Identifier conversion
/// here is a temporary bridge to relational storage not yet migrated to name types.
///
/// @author Steve Ebersole
public final class PhysicalNamingStrategyHelper {
	private PhysicalNamingStrategyHelper() {
	}

	public static PhysicalNamingContext context(JdbcEnvironment environment) {
		final var factory = environment.getIdentifierHelper().getPhysicalNameFactory();
		return () -> factory;
	}

	public static LogicalName logicalName(Identifier name) {
		return name == null ? null : new LogicalName( name.getText(), name.isQuoted(), name.isExplicit() );
	}

	/// Temporary projection for consumers whose storage still uses Identifier.
	public static Identifier identifier(LogicalName name) {
		return name == null ? null : new Identifier( name.getText(), name.isQuoted(), name.isExplicit() );
	}

	/// Temporary projection of a resolved name into unmigrated relational storage.
	public static Identifier identifier(PhysicalName name, LogicalName logicalName) {
		return name == null ? null : new Identifier( name.getText(), name.isQuoted(), logicalName != null && logicalName.isExplicit() );
	}

	/// Project finalized spelling into a consumer whose physical storage is not yet migrated.
	public static Identifier physicalIdentifier(PhysicalName name) {
		return name == null ? null : new Identifier( name.getText(), name.isQuoted() );
	}

	public static PhysicalName resolve(
			LogicalName logicalName,
			JdbcEnvironment environment,
			BiFunction<LogicalName, PhysicalNamingContext, PhysicalName> callback,
			String role,
			boolean optional) {
		if ( logicalName == null && !optional ) {
			throw new MappingException( "Missing logical " + role + " name" );
		}
		final var context = context( environment );
		final var result = callback.apply( logicalName, context );
		if ( result == null ) {
			if ( optional ) {
				return null;
			}
			throw new MappingException( "Physical naming strategy returned null for " + role + " '" + logicalName + "'" );
		}
		final boolean requestedQuoting = result.isQuoted() || logicalName != null && logicalName.isQuoted();
		final var normalized = environment.getIdentifierHelper().toIdentifier( result.getText(), requestedQuoting );
		return context.getPhysicalNameFactory().create( normalized.getText(), requestedQuoting || normalized.isQuoted() );
	}

	private static Identifier legacy(PhysicalName name, Identifier logicalName) {
		return name == null ? null : new Identifier( name.getText(), name.isQuoted(), logicalName != null && logicalName.isExplicit() );
	}

	public static Identifier toPhysicalCatalogName(PhysicalNamingStrategy strategy, Identifier name, JdbcEnvironment environment) {
		return legacy( resolve( logicalName( name ), environment, strategy::toPhysicalCatalogName, "catalog", true ), name );
	}

	public static Identifier toPhysicalSchemaName(PhysicalNamingStrategy strategy, Identifier name, JdbcEnvironment environment) {
		return legacy( resolve( logicalName( name ), environment, strategy::toPhysicalSchemaName, "schema", true ), name );
	}

	public static Identifier toPhysicalTableName(PhysicalNamingStrategy strategy, Identifier name, JdbcEnvironment environment) {
		return legacy( resolve( logicalName( name ), environment, strategy::toPhysicalTableName, "table", false ), name );
	}

	public static Identifier toPhysicalSequenceName(PhysicalNamingStrategy strategy, Identifier name, JdbcEnvironment environment) {
		return legacy( resolve( logicalName( name ), environment, strategy::toPhysicalSequenceName, "sequence", false ), name );
	}

	public static Identifier toPhysicalColumnName(PhysicalNamingStrategy strategy, Identifier name, JdbcEnvironment environment) {
		return legacy( resolve( logicalName( name ), environment, strategy::toPhysicalColumnName, "column", false ), name );
	}

	public static Identifier toPhysicalTypeName(PhysicalNamingStrategy strategy, Identifier name, JdbcEnvironment environment) {
		return legacy( resolve( logicalName( name ), environment, strategy::toPhysicalTypeName, "type", false ), name );
	}
}
