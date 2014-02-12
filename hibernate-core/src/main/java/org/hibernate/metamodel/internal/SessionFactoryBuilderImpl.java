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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Settings;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.DefaultCustomEntityDirtinessStrategy;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.StandardEntityNotFoundDelegate;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class SessionFactoryBuilderImpl implements SessionFactoryBuilder {
	private final MetadataImplementor metadata;
	private final SessionFactoryOptionsImpl options;

	SessionFactoryBuilderImpl(MetadataImplementor metadata, StandardServiceRegistry serviceRegistry) {
		this.metadata = metadata;
		options = new SessionFactoryOptionsImpl( serviceRegistry );
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
	public SessionFactoryBuilder with(EntityMode entityMode, Class<? extends EntityTuplizer> tuplizerClass) {
		this.options.settings.getEntityTuplizerFactory().registerDefaultTuplizerClass( entityMode, tuplizerClass );
		return this;
	}

	@Override
	public SessionFactoryBuilder withValidatorFactory(Object validatorFactory) {
		this.options.validatorFactoryReference = validatorFactory;
		return this;
	}

	@Override
	public SessionFactoryBuilder withBeanManager(Object beanManager) {
		this.options.beanManagerReference = beanManager;
		return this;
	}

	@Override
	public SessionFactory build() {
		return new SessionFactoryImpl( metadata, options );
	}

	private static class SessionFactoryOptionsImpl implements SessionFactory.SessionFactoryOptions {
		private final StandardServiceRegistry serviceRegistry;

		private Interceptor interceptor;
		private CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
		private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;
		private List<SessionFactoryObserver> sessionFactoryObserverList = new ArrayList<SessionFactoryObserver>();
		private List<EntityNameResolver> entityNameResolvers = new ArrayList<EntityNameResolver>();
		private EntityNotFoundDelegate entityNotFoundDelegate;
		private Settings settings;
		public Object beanManagerReference;
		public Object validatorFactoryReference;

		public SessionFactoryOptionsImpl(StandardServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;

			final Map configurationSettings = serviceRegistry.getService( ConfigurationService.class ).getSettings();

			final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );

			this.interceptor = strategySelector.resolveDefaultableStrategy(
					Interceptor.class,
					configurationSettings.get( AvailableSettings.INTERCEPTOR ),
					EmptyInterceptor.INSTANCE
			);

			this.entityNotFoundDelegate = StandardEntityNotFoundDelegate.INSTANCE;

			this.customEntityDirtinessStrategy = strategySelector.resolveDefaultableStrategy(
					CustomEntityDirtinessStrategy.class,
					configurationSettings.get( AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY ),
					DefaultCustomEntityDirtinessStrategy.INSTANCE
			);

			this.currentTenantIdentifierResolver = strategySelector.resolveStrategy(
					CurrentTenantIdentifierResolver.class,
					configurationSettings.get( AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER )
			);

			this.beanManagerReference = configurationSettings.get( "javax.persistence.bean.manager" );
			this.validatorFactoryReference = configurationSettings.get( "javax.persistence.validation.factory" );

			Properties properties = new Properties();
			properties.putAll( configurationSettings );
			this.settings = new SettingsFactory().buildSettings( properties, serviceRegistry );
		}

		@Override
		public StandardServiceRegistry getServiceRegistry() {
			return serviceRegistry;
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
		public Settings getSettings() {
			return settings;
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

		@Override
		public Object getBeanManagerReference() {
			return beanManagerReference;
		}

		@Override
		public Object getValidatorFactoryReference() {
			return validatorFactoryReference;
		}
	}

}