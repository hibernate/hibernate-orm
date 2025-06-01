/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import static org.hibernate.internal.util.StringHelper.unqualify;

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
		return unqualify( entityNaming.getEntityName() );
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
		else {
			return super.determineJoinColumnName( source );
		}
	}

	@Override
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		if ( source.getAssociationOwningAttributePath() != null ) {
			final String name = source.getOwningPhysicalTableName()
					+ '_'
					+ transformAttributePath( source.getAssociationOwningAttributePath() );
			return toIdentifier( name, source.getBuildingContext() );
		}
		else {
			return super.determineJoinTableName( source );
		}
	}
}
