/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.engine.config.spi.ConfigurationService;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;

import static org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper.physicalIdentifier;

/// Resolves and renders named SQL objects in the existing default type namespace.
///
/// @author Steve Ebersole
public final class NamedSqlTypeNames {
	private NamedSqlTypeNames() {
	}

	public static String resolveEnum(String logicalName, Database database) {
		return render( database.getDefaultNamespace().resolvePhysicalEnumName( Identifier.toIdentifier( logicalName ) ), database );
	}

	public static String resolveArray(String logicalName, Database database) {
		return render( database.getDefaultNamespace().resolvePhysicalArrayName( Identifier.toIdentifier( logicalName ) ), database );
	}

	private static String render(Identifier name, Database database) {
		final var namespace = database.getDefaultNamespace();
		final var qualifiers = namespace.getPhysicalName();
		return SqlStringGenerationContextImpl.fromConfigurationMap( database.getJdbcEnvironment(), database,
				database.getServiceRegistry().requireService( ConfigurationService.class ).getSettings() )
				.format( new QualifiedNameImpl(
						physicalIdentifier( qualifiers.catalog() ),
						physicalIdentifier( qualifiers.schema() ), name ) );
	}
}
