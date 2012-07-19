/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
 * Boston, MA  02110-1301  USA\
 */
package org.hibernate.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.ProviderChecker;

/**
 * The Hibernate {@link PersistenceProvider} implementation
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class HibernatePersistenceProvider implements PersistenceProvider {
    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(
			EntityManagerMessageLogger.class,
			HibernatePersistenceProvider.class.getName()
	);

	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Note: per-spec, the values passed as {@code properties} override values found in {@code persistence.xml}
	 */
	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		final Map integration = wrap( properties );
		final List<ParsedPersistenceXmlDescriptor> units = PersistenceXmlParser.locatePersistenceUnits( integration );

		if ( persistenceUnitName == null && units.size() > 1 ) {
			// no persistence-unit name to look for was given and we found multiple persistence-units
			throw new PersistenceException( "No name provided and multiple persistence units found" );
		}

		for ( ParsedPersistenceXmlDescriptor persistenceUnit : units ) {
			boolean matches = persistenceUnitName == null || persistenceUnit.getName().equals( persistenceUnitName );
			if ( !matches ) {
				continue;
			}

			// See if we (Hibernate) are the persistence provider
			if ( ! ProviderChecker.isProvider( persistenceUnit, properties ) ) {
				continue;
			}

			return Bootstrap.getEntityManagerFactoryBuilder( persistenceUnit, integration ).buildEntityManagerFactory();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map wrap(Map properties) {
		return properties== null ? Collections.emptyMap() : Collections.unmodifiableMap( properties );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Note: per-spec, the values passed as {@code properties} override values found in {@link PersistenceUnitInfo}
	 */
	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map integration) {
		return Bootstrap.getEntityManagerFactoryBuilder( info, integration ).buildEntityManagerFactory();
	}

	private final ProviderUtil providerUtil = new ProviderUtil() {
		public LoadState isLoadedWithoutReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithoutReference( proxy, property, cache );
		}

		public LoadState isLoadedWithReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithReference( proxy, property, cache );
		}

		public LoadState isLoaded(Object o) {
			return PersistenceUtilHelper.isLoaded(o);
		}
	};

	@Override
	public ProviderUtil getProviderUtil() {
		return providerUtil;
	}

}