/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyResolverInitiator implements StandardServiceInitiator<PropertyAccessStrategyResolver> {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyResolverInitiator INSTANCE = new PropertyAccessStrategyResolverInitiator();

	@Override
	public Class<PropertyAccessStrategyResolver> getServiceInitiated() {
		return PropertyAccessStrategyResolver.class;
	}

	@Override
	public PropertyAccessStrategyResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new PropertyAccessStrategyResolverStandardImpl( registry );
	}
}
