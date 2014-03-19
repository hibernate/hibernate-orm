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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.ProviderChecker;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;

import org.jboss.logging.Logger;

/**
 * The Hibernate {@link PersistenceProvider} implementation
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class HibernatePersistenceProvider implements PersistenceProvider {
	private static final Logger log = Logger.getLogger( HibernatePersistenceProvider.class );

	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();
	
	/**
	 * {@inheritDoc}
	 * <p/>
	 * Note: per-spec, the values passed as {@code properties} override values found in {@code persistence.xml}
	 */
	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		log.tracef( "Starting createEntityManagerFactory for persistenceUnitName %s", persistenceUnitName );

		try {
			final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties );
			if ( builder == null ) {
				log.trace( "Could not obtain matching EntityManagerFactoryBuilder, returning null" );
				return null;
			}
			else {
				return builder.build();
			}
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (Exception e) {
			log.debug( "Unable to build entity manager factory", e );
			throw new PersistenceException( "Unable to build entity manager factory", e );
		}
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties) {
		return getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties, null );
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties, ClassLoader providedClassLoader) {
		log.tracef( "Attempting to obtain correct EntityManagerFactoryBuilder for persistenceUnitName : %s", persistenceUnitName );

		final Map integration = wrap( properties );
		final List<ParsedPersistenceXmlDescriptor> units;
		try {
			units = PersistenceXmlParser.locatePersistenceUnits( integration );
		}
		catch (Exception e) {
			log.debug( "Unable to locate persistence units", e );
			throw new PersistenceException( "Unable to locate persistence units", e );
		}

		log.debugf( "Located and parsed %s persistence units; checking each", units.size() );

		if ( persistenceUnitName == null && units.size() > 1 ) {
			// no persistence-unit name to look for was given and we found multiple persistence-units
			throw new PersistenceException( "No name provided and multiple persistence units found" );
		}

		for ( ParsedPersistenceXmlDescriptor persistenceUnit : units ) {
			log.debugf(
					"Checking persistence-unit [name=%s, explicit-provider=%s] against incoming persistence unit name [%s]",
					persistenceUnit.getName(),
					persistenceUnit.getProviderClassName(),
					persistenceUnitName
			);

			final boolean matches = persistenceUnitName == null || persistenceUnit.getName().equals( persistenceUnitName );
			if ( !matches ) {
				log.debug( "Excluding from consideration due to name mis-match" );
				continue;
			}

			// See if we (Hibernate) are the persistence provider
			if ( ! ProviderChecker.isProvider( persistenceUnit, properties ) ) {
				log.debug( "Excluding from consideration due to provider mis-match" );
				continue;
			}

			return getEntityManagerFactoryBuilder( persistenceUnit, integration, providedClassLoader );
		}

		log.debug( "Found no matching persistence units" );
		return null;
	}

	@SuppressWarnings("unchecked")
	protected static Map wrap(Map properties) {
		return properties == null ? Collections.emptyMap() : Collections.unmodifiableMap( properties );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Note: per-spec, the values passed as {@code properties} override values found in {@link PersistenceUnitInfo}
	 */
	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		log.tracef( "Starting createContainerEntityManagerFactory : %s", info.getPersistenceUnitName() );

		return getEntityManagerFactoryBuilder( info, properties ).build();
	}

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map map) {
		log.tracef( "Starting generateSchema : PUI.name=%s", info.getPersistenceUnitName() );

		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilder( info, map );
		builder.generateSchema();
	}

	@Override
	public boolean generateSchema(String persistenceUnitName, Map map) {
		log.tracef( "Starting generateSchema for persistenceUnitName %s", persistenceUnitName );

		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, map );
		if ( builder == null ) {
			log.trace( "Could not obtain matching EntityManagerFactoryBuilder, returning false" );
			return false;
		}
		builder.generateSchema();
		return true;
	}

	private EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceUnitInfo info, Map integration) {
		return getEntityManagerFactoryBuilder( new PersistenceUnitInfoDescriptor( info ), integration, null );
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map integration, ClassLoader providedClassLoader) {
		return Bootstrap.getEntityManagerFactoryBuilder( persistenceUnitDescriptor, integration, providedClassLoader );
	}

	private final ProviderUtil providerUtil = new ProviderUtil() {
		@Override
		public LoadState isLoadedWithoutReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithoutReference( proxy, property, cache );
		}
		@Override
		public LoadState isLoadedWithReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithReference( proxy, property, cache );
		}
		@Override
		public LoadState isLoaded(Object o) {
			return PersistenceUtilHelper.isLoaded(o);
		}
	};

	@Override
	public ProviderUtil getProviderUtil() {
		return providerUtil;
	}

}
