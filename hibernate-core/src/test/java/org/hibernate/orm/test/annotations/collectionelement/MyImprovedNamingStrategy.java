/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import jakarta.annotation.Nonnull;

import org.hibernate.boot.model.naming.spi.CollectionTableNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.model.source.spi.AttributePath;

import org.hibernate.boot.model.naming.spi.CollectionKeyNamingInput;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

public class MyImprovedNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
	@Override
	@Nonnull
	public LogicalName determineCollectionTableName(@Nonnull CollectionTableNamingInput source, @Nonnull ImplicitNamingContext context) {
		// This impl uses the owner entity table name instead of the JPA entity name when
		// generating the implicit name.
		final String name = ((NamedTableNamingInput) source.owningTable()).names().physicalName().getText()
				+ '_'
				+ transformAttributePath( AttributePath.parse( source.attributePath() ) );

		return context.implicitName( name );
	}

	@Override
	@Nonnull
	public LogicalName determineCollectionKeyColumnName(@Nonnull CollectionKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
		final var table = (NamedTableNamingInput) input.reference().table();
		return context.implicitName( table.names().physicalName().getText() + "_" + input.reference().column().logicalName().getText() );
	}

}
