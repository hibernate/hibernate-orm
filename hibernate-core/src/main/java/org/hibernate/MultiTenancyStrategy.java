/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

import java.util.Map;

import org.hibernate.cfg.Environment;

/**
 * Describes the methods for multi-tenancy understood by Hibernate.
 *
 * @author Steve Ebersole
 */
public enum MultiTenancyStrategy {
	/**
	 * Multi-tenancy implemented by use of discriminator columns.
	 */
	DISCRIMINATOR,
	/**
	 * Multi-tenancy implemented as separate schemas.
	 */
	SCHEMA,
	/**
	 * Multi-tenancy implemented as separate databases.
	 */
	DATABASE,
	/**
	 * No multi-tenancy
	 */
	NONE;

	public static MultiTenancyStrategy determineMultiTenancyStrategy(Map properties) {
		final Object strategy = properties.get( Environment.MULTI_TENANT );
		if ( strategy == null ) {
			return MultiTenancyStrategy.NONE;
		}

		if ( MultiTenancyStrategy.class.isInstance( strategy ) ) {
			return (MultiTenancyStrategy) strategy;
		}

		final String strategyName = strategy.toString();
		if ( MultiTenancyStrategy.DISCRIMINATOR.name().equals( strategyName ) ) {
			return MultiTenancyStrategy.DISCRIMINATOR;
		}
		else if ( MultiTenancyStrategy.SCHEMA.name().equals( strategyName ) ) {
			return MultiTenancyStrategy.SCHEMA;
		}
		else if ( MultiTenancyStrategy.DATABASE.name().equals( strategyName ) ) {
			return MultiTenancyStrategy.DATABASE;
		}
		else if ( MultiTenancyStrategy.NONE.name().equals( strategyName ) ) {
			return MultiTenancyStrategy.NONE;
		}
		else {
			// todo log?
			return MultiTenancyStrategy.NONE;
		}
	}
}
