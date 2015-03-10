/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
}
