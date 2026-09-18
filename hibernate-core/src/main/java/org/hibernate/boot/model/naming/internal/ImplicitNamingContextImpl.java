/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.internal;

import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
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
}
