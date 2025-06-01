/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource.Nature;

/**
 * Implementation of the ImplicitNamingStrategy contract which conforms to the
 * naming rules initially implemented by Hibernate for JPA 1.0, prior to many
 * things being clarified.
 * <p>
 * For a more JPA 2 compliant strategy, see/use {@link ImplicitNamingStrategyJpaCompliantImpl}
 * <p>
 * Corresponds roughly to the legacy org.hibernate.cfg.EJB3NamingStrategy class.
 *
 * @author Steve Ebersole
 */
public class ImplicitNamingStrategyLegacyJpaImpl extends ImplicitNamingStrategyJpaCompliantImpl {
	/**
	 * Singleton access
	 */
	public static final ImplicitNamingStrategyLegacyJpaImpl INSTANCE = new ImplicitNamingStrategyLegacyJpaImpl();

	@Override
	public Identifier determineCollectionTableName(ImplicitCollectionTableNameSource source) {
		final Identifier owningPhysicalTableName = source.getOwningPhysicalTableName();
		final Identifier identifier = toIdentifier(
				owningPhysicalTableName.getText()
				+ "_" + transformAttributePath( source.getOwningAttributePath() ),
				source.getBuildingContext()
		);
		return owningPhysicalTableName.isQuoted() ? Identifier.quote( identifier ) : identifier;
	}

	@Override
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		final String ownerPortion = source.getOwningPhysicalTableName();
		final String ownedPortion =
				source.getNonOwningPhysicalTableName() == null
						? transformAttributePath( source.getAssociationOwningAttributePath() )
						: source.getNonOwningPhysicalTableName();
		return toIdentifier( ownerPortion + "_" + ownedPortion, source.getBuildingContext() );
	}

	@Override
	public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
		// legacy JPA-based naming strategy preferred to use {TableName}_{ReferencedColumnName}
		// where JPA was later clarified to prefer {EntityName}_{ReferencedColumnName}.
		//
		// The spec-compliant one implements the clarified {EntityName}_{ReferencedColumnName}
		// naming.  Here we implement the older {TableName}_{ReferencedColumnName} naming
//		final String name;
//		if ( source.getNature() == Nature.ENTITY && source.getAttributePath() != null ) {
//			// many-to-one /  one-to-one
//			//
//			// legacy naming used the attribute name here, following suit with legacy hbm naming
//			//
//			// NOTE : attribute path being null here would be an error, so for now don't bother checking
//			name = transformAttributePath( source.getAttributePath() );
//		}
//		else if ( source.getNature() == Nature.ELEMENT_COLLECTION
		final String qualifier =
				source.getNature() == Nature.ELEMENT_COLLECTION || source.getAttributePath() == null
						? source.getReferencedTableName().getText()
						: transformAttributePath( source.getAttributePath() );
		final String name = qualifier + '_' + source.getReferencedColumnName().getText();
		return toIdentifier( name, source.getBuildingContext() );
	}
}
