/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.type.spi.TypeBootstrapContext;

/**
 * {@link BeanInstanceProducer} implementation for building beans related to custom types.
 *
 * @author Christian Beikov
 */
@Internal //TODO: move this to org.hibernate.boot.internal, where its only usage is
public class TypeBeanInstanceProducer implements BeanInstanceProducer, TypeBootstrapContext {
	private final ConfigurationService configurationService;

	public TypeBeanInstanceProducer(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	@Override
	public <B> B produceBeanInstance(Class<B> beanType) {
		try {
			final Constructor<B> bootstrapContextAwareTypeConstructor = ReflectHelper.getConstructor(
					beanType,
					TypeBootstrapContext.class
			);
			if ( bootstrapContextAwareTypeConstructor != null ) {
				return bootstrapContextAwareTypeConstructor.newInstance( this );
			}
			else {
				return beanType.newInstance();
			}
		}
		catch ( Exception e ) {
			throw new MappingException( "Could not instantiate Type: " + beanType.getName(), e );
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
}
