/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.SPI;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.model.naming.spi.AssociationTableNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.relational.naming.spi.LogicalName;

/// JPA-oriented naming variant using mapped physical table names for association tables.
///
/// @author Steve Ebersole
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public class ImplicitNamingStrategyJpaCompliantImpl extends StandardImplicitNamingStrategy {
	public static final ImplicitNamingStrategy INSTANCE = new ImplicitNamingStrategyJpaCompliantImpl();

	@Override
	protected String transformAttributePath(org.hibernate.boot.model.source.spi.AttributePath attributePath) {
		return attributePath.getProperty();
	}

	@Override
	public LogicalName determineAssociationTableName(AssociationTableNamingInput input, ImplicitNamingContext context) {
		final var owner = input.owningTable();
		final var target = input.targetTable();
		final var ownerName = owner instanceof NamedTableNamingInput named ? named.names().physicalName() : null;
		final var targetName = target instanceof NamedTableNamingInput named ? named.names().physicalName() : null;
		return context.implicitName(
				(ownerName == null ? owner.logicalName().getText() : ownerName.getText()) + '_'
						+ (targetName == null ? target.logicalName().getText() : targetName.getText()),
				(ownerName == null ? owner.logicalName().isQuoted() : ownerName.isQuoted())
						|| (targetName == null ? target.logicalName().isQuoted() : targetName.isQuoted()) );
	}
}
