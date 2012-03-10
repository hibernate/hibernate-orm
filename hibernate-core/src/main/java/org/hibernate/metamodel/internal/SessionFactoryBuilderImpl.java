/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.internal.DefaultCustomEntityDirtinessStrategy;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.config.StrategyInstanceResolver;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.config.spi.ConfigurationService;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class SessionFactoryBuilderImpl implements SessionFactoryBuilder {
	private final MetadataImplementor metadata;
	private final SessionFactoryOptionsImpl options;

	SessionFactoryBuilderImpl(MetadataImplementor metadata) {
		this.metadata = metadata;
		options = new SessionFactoryOptionsImpl( metadata.getServiceRegistry() );
	}

	@Override
	public SessionFactoryBuilder with(Interceptor interceptor) {
		this.options.interceptor = interceptor;
		return this;
	}

	@Override
	public SessionFactoryBuilder with(CustomEntityDirtinessStrategy dirtinessStrategy) {
		this.options.customEntityDirtinessStrategy = dirtinessStrategy;
		return this;
	}

	@Override
	public SessionFactoryBuilder with(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
		this.options.currentTenantIdentifierResolver = currentTenantIdentifierResolver;
		return this;
	}

	@Override
	public SessionFactoryBuilder add(SessionFactoryObserver... observers) {
		this.options.sessionFactoryObserverList.addAll( Arrays.asList( observers ) );
		return this;
	}

	@Override
	public SessionFactoryBuilder add(EntityNameResolver... entityNameResolvers) {
		this.options.entityNameResolvers.addAll( Arrays.asList( entityNameResolvers ) );
		return this;
	}

	@Override
	public SessionFactoryBuilder with(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.options.entityNotFoundDelegate = entityNotFoundDelegate;
		return this;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return new SessionFactoryImpl( metadata, options );
	}

	private static class SessionFactoryOptionsImpl implements SessionFactory.SessionFactoryOptions {
		private Interceptor interceptor;
		private CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
		private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;
		private List<SessionFactoryObserver> sessionFactoryObserverList = new ArrayList<SessionFactoryObserver>();
		private List<EntityNameResolver> entityNameResolvers = new ArrayList<EntityNameResolver>();
		private EntityNotFoundDelegate entityNotFoundDelegate;

		public SessionFactoryOptionsImpl(ServiceRegistry serviceRegistry) {
			final StrategyInstanceResolver strategyInstanceResolver = new StrategyInstanceResolver(
					serviceRegistry.getService( ClassLoaderService.class )
			);

			final Map configurationSettings = serviceRegistry.getService( ConfigurationService.class ).getSettings();

			this.interceptor = strategyInstanceResolver.resolveDefaultableStrategyInstance(
					configurationSettings.get( AvailableSettings.INTERCEPTOR ),
					Interceptor.class,
					EmptyInterceptor.INSTANCE
			);

			// TODO: should there be a DefaultEntityNotFoundDelegate.INSTANCE?
			this.entityNotFoundDelegate = new EntityNotFoundDelegate() {
				public void handleEntityNotFound(String entityName, Serializable id) {
					throw new ObjectNotFoundException( id, entityName );
				}
			};

			this.customEntityDirtinessStrategy = strategyInstanceResolver.resolveDefaultableStrategyInstance(
					configurationSettings.get( AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY ),
					CustomEntityDirtinessStrategy.class,
					DefaultCustomEntityDirtinessStrategy.INSTANCE
			);

			this.currentTenantIdentifierResolver = strategyInstanceResolver.resolveStrategyInstance(
					configurationSettings.get( AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER  ),
					CurrentTenantIdentifierResolver.class
			);
		}

		@Override
		public Interceptor getInterceptor() {
			return interceptor;
		}

		@Override
		public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
			return customEntityDirtinessStrategy;
		}

		@Override
		public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
			return currentTenantIdentifierResolver;
		}

		@Override
		public SessionFactoryObserver[] getSessionFactoryObservers() {
			return sessionFactoryObserverList.toArray( new SessionFactoryObserver[sessionFactoryObserverList.size()] );
		}

		@Override
		public EntityNameResolver[] getEntityNameResolvers() {
			return entityNameResolvers.toArray( new EntityNameResolver[entityNameResolvers.size()] );
		}

		@Override
		public EntityNotFoundDelegate getEntityNotFoundDelegate() {
			return entityNotFoundDelegate;
		}
	}

}