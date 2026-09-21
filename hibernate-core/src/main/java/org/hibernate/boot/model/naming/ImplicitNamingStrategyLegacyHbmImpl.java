/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.naming.spi.AssociationKeyNamingInput;
import org.hibernate.boot.model.naming.spi.JoinColumnNamingInput;

import org.hibernate.boot.model.naming.spi.AssociationTableNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.model.source.spi.AttributePath;

import org.hibernate.SPI;

import static org.hibernate.internal.util.StringHelper.unqualify;

/**
 * Implements the original legacy naming behavior.
 *
 * @deprecated Use {@link org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy}
 * or {@link ImplicitNamingStrategyJpaCompliantImpl}. Verify mapping names when migrating.
 *
 * @author Steve Ebersole
 */
@Deprecated(since = "9.0", forRemoval = true)
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class ImplicitNamingStrategyLegacyHbmImpl extends ImplicitNamingStrategyJpaCompliantImpl {
	@SPI(SPI.Role.USE)
	public ImplicitNamingStrategyLegacyHbmImpl() {
	}

	/**
	 * Singleton access
	 */
	public static final ImplicitNamingStrategyLegacyHbmImpl INSTANCE = new ImplicitNamingStrategyLegacyHbmImpl();

	@Override
	protected String transformEntityName(EntityNaming entityNaming) {
		return unqualify( entityNaming.getEntityName() );
	}


	@Override
	public LogicalName determineJoinColumnName(JoinColumnNamingInput input, ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) );
	}

	@Override
	public LogicalName determineAssociationKeyColumnName(AssociationKeyNamingInput input, ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) );
	}


	@Override
	public LogicalName determineCollectionKeyColumnName(org.hibernate.boot.model.naming.spi.CollectionKeyNamingInput input, ImplicitNamingContext context) {
		if ( input.kind() == org.hibernate.boot.model.naming.spi.CollectionKeyNamingInput.Kind.ONE_TO_MANY ) {
			return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) );
		}
		return input.inverseAttributePath().map( path -> context.implicitName( transformAttributePath( AttributePath.parse( path ) ) ) )
				.orElseGet( () -> super.determineCollectionKeyColumnName( input, context ) );
	}

	@Override
	public LogicalName determineAssociationTableName(AssociationTableNamingInput source, ImplicitNamingContext context) {
		final var associationOwningAttributePath = AttributePath.parse( source.attributePath() );
		if ( associationOwningAttributePath != null ) {
			final String name = (source.owningTable() instanceof NamedTableNamingInput named
					? named.names().physicalName().getText() : source.owningTable().logicalName().getText())
					+ '_'
					+ transformAttributePath( associationOwningAttributePath );
			return context.implicitName( name );
		}
		else {
			return super.determineAssociationTableName( source, context );
		}
	}
}
