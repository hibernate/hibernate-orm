/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.internal.util.StringHelper;

/**
 * Implements the original legacy naming behavior.
 *
 * @author Steve Ebersole
 */
public class ImplicitNamingStrategyLegacyHbmImpl extends ImplicitNamingStrategyJpaCompliantImpl {
	/**
	 * Singleton access
	 */
	public static final ImplicitNamingStrategyLegacyHbmImpl INSTANCE = new ImplicitNamingStrategyLegacyHbmImpl();

	@Override
	protected String transformEntityName(EntityNaming entityNaming) {
		return StringHelper.unqualify( entityNaming.getEntityName() );
	}

	@Override
	public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
		return source.isCollectionElement()
				? toIdentifier( "elt", source.getBuildingContext() )
				: super.determineBasicColumnName( source );
	}

	@Override
	public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
		if ( source.getAttributePath() != null ) {
			return toIdentifier(
					transformAttributePath( source.getAttributePath() ),
					source.getBuildingContext()
			);
		}

		return super.determineJoinColumnName( source );
	}


	@Override
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		if ( source.getAssociationOwningAttributePath() != null ) {
			final String name = source.getOwningPhysicalTableName()
					+ '_'
					+ transformAttributePath( source.getAssociationOwningAttributePath() );

			return toIdentifier( name, source.getBuildingContext() );
		}

		return super.determineJoinTableName( source );
	}
}
