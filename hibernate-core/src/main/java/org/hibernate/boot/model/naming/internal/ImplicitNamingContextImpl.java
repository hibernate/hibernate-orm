/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.internal;

import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.identifier.spi.DelegatingIdentifierHelper;
import org.hibernate.boot.model.naming.spi.ImplicitNamingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

/// Focused naming capabilities extracted from the current mapping context.
///
/// @author Steve Ebersole
public record ImplicitNamingContextImpl(
		ImplicitNamingDefaults getNamingDefaults,
		IdentifierHelper getIdentifierHelper,
		String getSchemaCharset) implements ImplicitNamingContext {
	public static ImplicitNamingContext from(MetadataBuildingContext context) {
		return new ImplicitNamingContextImpl(
				context.getEffectiveDefaults(),
				context.getMetadataCollector().getDatabase().getJdbcEnvironment().getIdentifierHelper(),
				context.getBuildingPlan().getSchemaCharset()
		);
	}
	/// Logical table-name generation must not apply database quoting before physical naming.
	public static ImplicitNamingContext forPhysicalNaming(MetadataBuildingContext context) {
		final var original = from( context );
		final var helper = new DelegatingIdentifierHelper( original.getIdentifierHelper() ) {
			@Override
			public Identifier toIdentifier(String text) {
				return Identifier.toIdentifier( text, false, false, false );
			}

			@Override
			public Identifier toIdentifier(String text, boolean quoted) {
				return Identifier.toIdentifier( text, quoted, false, false );
			}

			@Override
			public Identifier toIdentifier(String text, boolean quoted, boolean explicit) {
				return Identifier.toIdentifier( text, quoted, false, explicit );
			}

			@Override
			public Identifier normalizeQuoting(Identifier identifier) {
				return identifier;
			}
		};
		return new ImplicitNamingContextImpl( original.getNamingDefaults(), helper, original.getSchemaCharset() );
	}

}
