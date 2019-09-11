/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.Instantiator;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.map.MapProxyFactory;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardMapEntityRepresentationStrategy implements EntityRepresentationStrategy {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StandardMapEntityRepresentationStrategy.class );

	private final JavaTypeDescriptor<Map> mapJtd;

	private final ProxyFactory proxyFactory;
	private final DynamicMapInstantiator instantiator;

	private final Map<String, PropertyAccess> propertyAccessMap = new ConcurrentHashMap<>();

	public StandardMapEntityRepresentationStrategy(
			PersistentClass bootType,
			RuntimeModelCreationContext creationContext) {
		this.mapJtd = creationContext.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( Map.class );

		this.proxyFactory = createProxyFactory( bootType );
		this.instantiator = new DynamicMapInstantiator( bootType );

		//noinspection unchecked
		final Iterator<Property> itr = bootType.getPropertyClosureIterator();
		int i = 0;
		while ( itr.hasNext() ) {
			//TODO: redesign how PropertyAccessors are acquired...
			final Property property = itr.next();
			final PropertyAccess propertyAccess = PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
					null,
					property.getName()
			);

			propertyAccessMap.put( property.getName(), propertyAccess );

			i++;
		}

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
				bootAttributeDescriptor.getName()
		);
	}

	@Override
	public Instantiator getInstantiator() {
		return instantiator;
	}

	@Override
	public ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	@Override
	public JavaTypeDescriptor<?> getMappedJavaTypeDescriptor() {
		return mapJtd;
	}

	@Override
	public JavaTypeDescriptor<?> getProxyJavaTypeDescriptor() {
		return null;
	}

	@Override
	public void visitEntityNameResolvers(Consumer<EntityNameResolver> consumer) {
		consumer.accept( DynamicMapInstantiator.ENTITY_NAME_RESOLVER );
	}
}
