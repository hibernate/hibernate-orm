/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.boot.spi;

import java.util.Map;

import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

import org.jboss.logging.Logger;

/**
 * Helper for handling checks to see whether Hibernate is the requested
 * {@link javax.persistence.spi.PersistenceProvider}
 *
 * @author Steve Ebersole
 */
public final class ProviderChecker {
	private static final Logger log = Logger.getLogger( ProviderChecker.class );

	@SuppressWarnings("deprecation")
	private static String[] HIBERNATE_PROVIDER_NAMES = new String[] {
			HibernatePersistenceProvider.class.getName(),
			HibernatePersistence.class.getName()
	};

	/**
	 * Does the descriptor and/or integration request Hibernate as the
	 * {@link javax.persistence.spi.PersistenceProvider}?  Note that in the case of no requested provider being named
	 * we assume we are the provider (the calls got to us somehow...)
	 *
	 * @param persistenceUnit The {@code <persistence-unit/>} descriptor.
	 * @param integration The integration values.
	 *
	 * @return {@code true} if Hibernate should be the provider; {@code false} otherwise.
	 */
	public static boolean isProvider(PersistenceUnitDescriptor persistenceUnit, Map integration) {
		// See if we (Hibernate) are the persistence provider
		return hibernateProviderNamesContain( extractRequestedProviderName( persistenceUnit, integration ) );
	}

	/**
	 * Is the requested provider name one of the recognized Hibernate provider names?
	 *
	 * @param requestedProviderName The requested provider name to check against the recognized Hibernate names.
	 *
	 * @return {@code true} if Hibernate should be the provider; {@code false} otherwise.
	 */
	public static boolean hibernateProviderNamesContain(String requestedProviderName) {
		log.tracef(
				"Checking requested PersistenceProvider name [%s] against Hibernate provider names",
				requestedProviderName
		);

		for ( String hibernateProviderName : HIBERNATE_PROVIDER_NAMES ) {
			if ( requestedProviderName.equals( hibernateProviderName ) ) {
				return true;
			}
		}

		log.tracef( "Found no match against Hibernate provider names" );
		return false;
	}

	/**
	 * Extract the requested persistence provider name using the algorithm Hibernate uses.  Namely, a provider named
	 * in the 'integration' map (under the key '{@value AvailableSettings#PROVIDER}') is preferred, as per-spec, over
	 * value specified in persistence unit.
	 *
	 * @param persistenceUnit The {@code <persistence-unit/>} descriptor.
	 * @param integration The integration values.
	 *
	 * @return The extracted provider name, or {@code null} if none found.
	 */
	public static String extractRequestedProviderName(PersistenceUnitDescriptor persistenceUnit, Map integration) {
		final String integrationProviderName = extractProviderName( integration );
		if ( integrationProviderName != null ) {
			log.debugf( "Integration provided explicit PersistenceProvider [%s]", integrationProviderName );
			return integrationProviderName;
		}

		final String persistenceUnitRequestedProvider = extractProviderName( persistenceUnit );
		if ( persistenceUnitRequestedProvider != null ) {
			log.debugf(
					"Persistence-unit [%s] requested PersistenceProvider [%s]",
					persistenceUnit.getName(),
					persistenceUnitRequestedProvider
			);
			return persistenceUnitRequestedProvider;
		}

		// NOTE : if no provider requested we assume we are the provider (the calls got to us somehow...)
		log.debug( "No PersistenceProvider explicitly requested, assuming Hibernate" );
		return HibernatePersistenceProvider.class.getName();
	}

	private static String extractProviderName(Map integration) {
		if ( integration == null ) {
			return null;
		}
		final String setting = (String) integration.get( AvailableSettings.PROVIDER );
		return setting == null ? null : setting.trim();
	}

	private static String extractProviderName(PersistenceUnitDescriptor persistenceUnit) {
		final String persistenceUnitRequestedProvider = persistenceUnit.getProviderClassName();
		return persistenceUnitRequestedProvider == null ? null : persistenceUnitRequestedProvider.trim();
	}

	private ProviderChecker() {
	}
}
