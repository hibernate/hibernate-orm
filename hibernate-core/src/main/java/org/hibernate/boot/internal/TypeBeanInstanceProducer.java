/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.InstantiationException;
import org.hibernate.Internal;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.type.spi.TypeBootstrapContext;
import org.hibernate.service.ServiceRegistry;

import java.lang.reflect.Constructor;
import java.util.Map;

import static org.hibernate.internal.util.ReflectHelper.getConstructorOrNull;

/**
 * {@link BeanInstanceProducer} implementation for building beans related to custom types.
 *
 * @author Christian Beikov
 */
@Internal
public class TypeBeanInstanceProducer implements BeanInstanceProducer, TypeBootstrapContext {
	private final ConfigurationService configurationService;
	private final ServiceRegistry serviceRegistry;

	public TypeBeanInstanceProducer(ConfigurationService configurationService, ServiceRegistry serviceRegistry) {
		this.configurationService = configurationService;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public <B> B produceBeanInstance(Class<B> beanType) {
		final Constructor<? extends B> bootstrapContextAwareConstructor =
				getConstructorOrNull( beanType, TypeBootstrapContext.class );
		if ( bootstrapContextAwareConstructor != null ) {
			try {
				return bootstrapContextAwareConstructor.newInstance( this );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate type", beanType, e );
			}
		}
		else {
			final Constructor<? extends B> constructor = getConstructorOrNull( beanType );
			if ( constructor != null ) {
				try {
					return constructor.newInstance();
				}
				catch ( Exception e ) {
					throw new InstantiationException( "Could not instantiate type", beanType, e );
				}
			}
			else {
				throw new InstantiationException( "No appropriate constructor for type", beanType );
			}
		}
	}

	@Override
	public <B> B produceBeanInstance(String name, Class<B> beanType) {
		return produceBeanInstance( beanType );
	}

	@Override
	public Map<String, Object> getConfigurationSettings() {
		return configurationService.getSettings();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
}
