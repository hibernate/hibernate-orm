/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal;

import java.io.Serializable;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.spi.LoadState;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitUtilImpl implements PersistenceUnitUtil, Serializable {
	private static final Logger log = Logger.getLogger( PersistenceUnitUtilImpl.class );

	private final SessionFactoryImplementor sessionFactory;
	private final transient PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	public PersistenceUnitUtilImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean isLoaded(Object entity, String attributeName) {
		// added log message to help with HHH-7454, if state == LoadState,NOT_LOADED, returning true or false is not accurate.
		log.debug( "PersistenceUnitUtil#isLoaded is not always accurate; consider using EntityManager#contains instead" );
		LoadState state = PersistenceUtilHelper.isLoadedWithoutReference( entity, attributeName, cache );
		if ( state == LoadState.LOADED ) {
			return true;
		}
		else if ( state == LoadState.NOT_LOADED ) {
			return false;
		}
		else {
			return PersistenceUtilHelper.isLoadedWithReference(
					entity,
					attributeName,
					cache
			) != LoadState.NOT_LOADED;
		}
	}

	@Override
	public boolean isLoaded(Object entity) {
		// added log message to help with HHH-7454, if state == LoadState,NOT_LOADED, returning true or false is not accurate.
		log.debug( "PersistenceUnitUtil#isLoaded is not always accurate; consider using EntityManager#contains instead" );
		return PersistenceUtilHelper.isLoaded( entity ) != LoadState.NOT_LOADED;
	}

	@Override
	public Object getIdentifier(Object entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Passed entity cannot be null" );
		}

		if ( entity instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) entity;
			return proxy.getHibernateLazyInitializer().getIdentifier();
		}
		else if ( entity instanceof ManagedEntity ) {
			final ManagedEntity enhancedEntity = (ManagedEntity) entity;
			return enhancedEntity.$$_hibernate_getEntityEntry().getId();
		}
		else {
			log.debugf(
					"javax.persistence.PersistenceUnitUtil.getIdentifier is only intended to work with enhanced entities " +
							"(although Hibernate also adapts this support to its proxies); " +
							"however the passed entity was not enhanced (nor a proxy).. may not be able to read identifier"
			);
			final Class entityClass = Hibernate.getClass( entity );
			final EntityPersister persister = sessionFactory.getMetamodel().entityPersister( entityClass );
			if ( persister == null ) {
				throw new IllegalArgumentException( entityClass + " is not an entity" );
			}
			//TODO does that work for @IdClass?
			return persister.getIdentifier( entity );
		}
	}
}
