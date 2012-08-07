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

import org.jboss.logging.Logger;

import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;

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

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MultiTenancyStrategy.class.getName()
	);

	public boolean requiresMultiTenantConnectionProvider() {
		return this == DATABASE || this == SCHEMA;
	}

	public static MultiTenancyStrategy determineMultiTenancyStrategy(Map properties) {
		final Object strategy = properties.get( Environment.MULTI_TENANT );
		if ( strategy == null ) {
			return MultiTenancyStrategy.NONE;
		}

		if ( MultiTenancyStrategy.class.isInstance( strategy ) ) {
			return (MultiTenancyStrategy) strategy;
		}

		final String strategyName = strategy.toString();
		try {
			return MultiTenancyStrategy.valueOf( strategyName.toUpperCase() );
		}
		catch ( RuntimeException e ) {
			LOG.warn( "Unknown multi tenancy strategy [ " +strategyName +" ], using MultiTenancyStrategy.NONE." );
			return MultiTenancyStrategy.NONE;
		}
	}
}
