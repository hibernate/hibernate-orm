/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.naming.spi.CollectionKeyNamingInput;

import org.hibernate.boot.model.naming.spi.CollectionTableNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.model.source.spi.AttributePath;

import org.hibernate.SPI;

/**
 * Implementation of the ImplicitNamingStrategy contract which conforms to the
 * naming rules initially implemented by Hibernate for JPA 1.0, prior to many
 * things being clarified.
 * <p>
 * For a more JPA 2 compliant strategy, see/use {@link ImplicitNamingStrategyJpaCompliantImpl}
 * <p>
 * Corresponds roughly to the legacy org.hibernate.cfg.EJB3NamingStrategy class.
 *
 * @deprecated Use {@link org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy}
 * or {@link ImplicitNamingStrategyJpaCompliantImpl}. Verify mapping names when migrating.
 *
 * @author Steve Ebersole
 */
@Deprecated(since = "9.0", forRemoval = true)
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class ImplicitNamingStrategyLegacyJpaImpl extends ImplicitNamingStrategyJpaCompliantImpl {
	@SPI(SPI.Role.USE)
	public ImplicitNamingStrategyLegacyJpaImpl() {
	}

	/**
	 * Singleton access
	 */
	public static final ImplicitNamingStrategyLegacyJpaImpl INSTANCE = new ImplicitNamingStrategyLegacyJpaImpl();

	@Override
	public LogicalName determineCollectionTableName(CollectionTableNamingInput input, ImplicitNamingContext context) {
		final var table = input.owningTable();
		final var physical = table instanceof NamedTableNamingInput named ? named.names().physicalName() : null;
		return context.implicitName(
				(physical == null ? table.logicalName().getText() : physical.getText())
						+ '_' + transformAttributePath( AttributePath.parse( input.attributePath() ) ),
				physical == null ? table.logicalName().isQuoted() : physical.isQuoted() );
	}


	@Override
	public LogicalName determineCollectionKeyColumnName(CollectionKeyNamingInput input, ImplicitNamingContext context) {
		if ( input.kind() == CollectionKeyNamingInput.Kind.TO_ONE_TABLE
				|| input.kind() == CollectionKeyNamingInput.Kind.ONE_TO_MANY
				|| input.inverseAttributePath().isPresent() ) {
			return super.determineCollectionKeyColumnName( input, context );
		}
		final var table = input.reference().table();
		final var physical = table instanceof NamedTableNamingInput named ? named.names().physicalName() : null;
		return joinName( physical == null ? table.logicalName().getText() : physical.getText(), input.reference(), context );
	}
}
