/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.dialect.Dialect;

/**
 * Implementation of the ImplicitNamingStrategy contract which conforms to the
 * naming rules initially implemented by Hibernate for JPA 1.0, prior to many
 * things being clarified.
 * <p/>
 * For a more JPA 2 compliant strategy, see/use {@link ImplicitNamingStrategyJpaCompliantImpl}
 * <p/>
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
		Identifier identifier = toIdentifier(
				source.getOwningPhysicalTableName().getText() + "_" + transformAttributePath( source.getOwningAttributePath() ),
				source.getBuildingContext()
		);
		if ( source.getOwningPhysicalTableName().isQuoted() ) {
			identifier = Identifier.quote( identifier );
		}
		return identifier;
	}

	@Override
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		final String ownerPortion = source.getOwningPhysicalTableName();
		final String ownedPortion;
		if ( source.getNonOwningPhysicalTableName() != null ) {
			ownedPortion = source.getNonOwningPhysicalTableName();
		}
		else {
			ownedPortion = transformAttributePath( source.getAssociationOwningAttributePath() );
		}

		return toIdentifier( ownerPortion + "_" + ownedPortion, source.getBuildingContext() );
	}

	@Override
	public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
		// legacy JPA-based naming strategy preferred to use {TableName}_{ReferencedColumnName}
		// where JPA was later clarified to prefer {EntityName}_{ReferencedColumnName}.
		//
		// The spec-compliant one implements the clarified {EntityName}_{ReferencedColumnName}
		// naming.  Here we implement the older {TableName}_{ReferencedColumnName} naming
		final String name;

//		if ( source.getNature() == ImplicitJoinColumnNameSource.Nature.ENTITY
//				&& source.getAttributePath() != null ) {
//			// many-to-one /  one-to-one
//			//
//			// legacy naming used the attribute name here, following suit with legacy hbm naming
//			//
//			// NOTE : attribute path being null here would be an error, so for now don't bother checking
//			name = transformAttributePath( source.getAttributePath() );
//		}
//		else if ( source.getNature() == ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION
		if ( source.getNature() == ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION
				|| source.getAttributePath() == null ) {
			name = source.getReferencedTableName().getText()
					+ '_'
					+ source.getReferencedColumnName().getText();
		}
		else {
			name = transformAttributePath( source.getAttributePath() )
					+ '_'
					+ source.getReferencedColumnName().getText();
		}

		return toIdentifier( name, source.getBuildingContext() );
	}
}
