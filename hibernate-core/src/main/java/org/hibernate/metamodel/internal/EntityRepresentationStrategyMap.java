/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.map.MapProxyFactory;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EntityRepresentationStrategyMap implements EntityRepresentationStrategy {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityRepresentationStrategyMap.class );

	private final JavaType<Map<String,?>> mapJavaType;

	private final ProxyFactory proxyFactory;
	private final EntityInstantiatorDynamicMap instantiator;

	public EntityRepresentationStrategyMap(
			PersistentClass bootType,
			RuntimeModelCreationContext creationContext) {
		this.mapJavaType = creationContext.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( Map.class );

		this.proxyFactory = createProxyFactory( bootType );
		this.instantiator = new EntityInstantiatorDynamicMap( bootType );

		createProxyFactory( bootType );
	}

	private static ProxyFactory createProxyFactory(PersistentClass bootType) {
		try {
			ProxyFactory proxyFactory = new MapProxyFactory();
			proxyFactory.postInstantiate(
					bootType.getEntityName(),
					null,
					null,
					null,
					null,
					null
			);
			return proxyFactory;
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( bootType.getEntityName(), he );
			return null;
		}
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.MAP;
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return null;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
				null,
				bootAttributeDescriptor.getName(),
				true );
	}

	@Override
	public EntityInstantiator getInstantiator() {
		return instantiator;
	}

	@Override
	public ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return mapJavaType;
	}

	@Override
	public JavaType<?> getProxyJavaType() {
		return null;
	}

	@Override
	public void visitEntityNameResolvers(Consumer<EntityNameResolver> consumer) {
		consumer.accept( EntityInstantiatorDynamicMap.ENTITY_NAME_RESOLVER );
	}
}
