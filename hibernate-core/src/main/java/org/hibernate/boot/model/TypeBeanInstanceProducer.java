/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.type.spi.TypeBootstrapContext;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class TypeBeanInstanceProducer implements BeanInstanceProducer, TypeBootstrapContext {

	private final TypeConfiguration typeConfiguration;

	public TypeBeanInstanceProducer(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public <B> B produceBeanInstance(Class<B> beanType) {
		try {
			final B type;
			final Constructor<B> bootstrapContextAwareTypeConstructor = ReflectHelper.getConstructor(
					beanType,
					TypeBootstrapContext.class
			);
			if ( bootstrapContextAwareTypeConstructor != null ) {
				type = bootstrapContextAwareTypeConstructor.newInstance( this );
			}
			else {
				type = beanType.newInstance();
			}
			return type;
		}
		catch (Exception e) {
			throw new MappingException( "Could not instantiate Type: " + beanType.getName(), e );
		}
	}

	@Override
	public <B> B produceBeanInstance(String name, Class<B> beanType) {
		return produceBeanInstance( beanType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getConfigurationSettings() {
		return typeConfiguration.getServiceRegistry().getService( ConfigurationService.class ).getSettings();
	}
}
