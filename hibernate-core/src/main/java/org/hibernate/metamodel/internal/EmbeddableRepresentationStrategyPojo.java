/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyIndexBackRefImpl;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.internal.CompositeUserTypeJavaTypeWrapper;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableRepresentationStrategyPojo extends AbstractEmbeddableRepresentationStrategy {
	private final StrategySelector strategySelector;

	private final ReflectionOptimizer reflectionOptimizer;
	private final EmbeddableInstantiator instantiator;

	public EmbeddableRepresentationStrategyPojo(
			Component bootDescriptor,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			EmbeddableInstantiator customInstantiator,
			CompositeUserType<Object> compositeUserType,
			RuntimeModelCreationContext creationContext) {
		super(
				bootDescriptor,
				resolveEmbeddableJavaType( bootDescriptor, compositeUserType, creationContext ),
				creationContext
		);


		assert bootDescriptor.getComponentClass() != null;

		this.strategySelector = creationContext.getServiceRegistry().getService( StrategySelector.class );

		this.reflectionOptimizer = buildReflectionOptimizer( bootDescriptor, creationContext );

		this.instantiator = customInstantiator != null
				? customInstantiator
				: determineInstantiator( bootDescriptor, runtimeDescriptorAccess, creationContext );
	}

	private static <T> JavaType<T> resolveEmbeddableJavaType(
			Component bootDescriptor,
			CompositeUserType<T> compositeUserType,
			RuntimeModelCreationContext creationContext) {
		final JavaTypeRegistry javaTypeRegistry = creationContext.getTypeConfiguration().getJavaTypeRegistry();
		if ( compositeUserType == null ) {
			return javaTypeRegistry.resolveDescriptor( bootDescriptor.getComponentClass() );
		}
		return javaTypeRegistry.resolveDescriptor(
				compositeUserType.returnedClass(),
				() -> new CompositeUserTypeJavaTypeWrapper<>( compositeUserType )
		);
	}

	private EmbeddableInstantiator determineInstantiator(
			Component bootDescriptor,
			Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
			RuntimeModelCreationContext creationContext) {
		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			final ReflectionOptimizer.InstantiationOptimizer instantiationOptimizer = reflectionOptimizer.getInstantiationOptimizer();
			return new EmbeddableInstantiatorPojoOptimized(
					getEmbeddableJavaType(),
					runtimeDescriptorAccess,
					instantiationOptimizer
			);
		}

		if ( bootDescriptor.isEmbedded() && ReflectHelper.isAbstractClass( bootDescriptor.getComponentClass() ) ) {
			return new EmbeddableInstantiatorProxied(
					bootDescriptor.getComponentClass(),
					runtimeDescriptorAccess,
					creationContext.getServiceRegistry()
							.getService( ProxyFactoryFactory.class )
							.buildBasicProxyFactory( bootDescriptor.getComponentClass() )
			);
		}

		return new EmbeddableInstantiatorPojoStandard( getEmbeddableJavaType(), runtimeDescriptorAccess );
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return reflectionOptimizer;
	}

	@Override
	protected PropertyAccess buildPropertyAccess(Property bootAttributeDescriptor) {
		PropertyAccessStrategy strategy = bootAttributeDescriptor.getPropertyAccessStrategy( getEmbeddableJavaType().getJavaTypeClass() );

		if ( strategy == null ) {
			final String propertyAccessorName = bootAttributeDescriptor.getPropertyAccessorName();
			if ( StringHelper.isNotEmpty( propertyAccessorName ) ) {

				// handle explicitly specified attribute accessor
				strategy = strategySelector.resolveStrategy(
						PropertyAccessStrategy.class,
						propertyAccessorName
				);
			}
			else {
				if ( bootAttributeDescriptor instanceof Backref ) {
					final Backref backref = (Backref) bootAttributeDescriptor;
					strategy = new PropertyAccessStrategyBackRefImpl( backref.getCollectionRole(), backref
							.getEntityName() );
				}
				else if ( bootAttributeDescriptor instanceof IndexBackref ) {
					final IndexBackref indexBackref = (IndexBackref) bootAttributeDescriptor;
					strategy = new PropertyAccessStrategyIndexBackRefImpl(
							indexBackref.getCollectionRole(),
							indexBackref.getEntityName()
					);
				}
				else {
					// for now...
					strategy = BuiltInPropertyAccessStrategies.MIXED.getStrategy();
				}
			}
		}

		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							getEmbeddableJavaType().getJavaType().getTypeName(),
							bootAttributeDescriptor.getName()
					)
			);
		}

		return strategy.buildPropertyAccess(
				getEmbeddableJavaType().getJavaTypeClass(),
				bootAttributeDescriptor.getName(),
				instantiator instanceof StandardEmbeddableInstantiator
		);
	}

	private ReflectionOptimizer buildReflectionOptimizer(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {

		if ( hasCustomAccessors() || bootDescriptor.getCustomInstantiator() != null || bootDescriptor.getInstantiator() != null ) {
			return null;
		}

		final Map<String, PropertyAccess> propertyAccessMap = new LinkedHashMap<>();

		int i = 0;
		for ( Property property : bootDescriptor.getProperties() ) {
			propertyAccessMap.put( property.getName(), getPropertyAccesses()[i] );
			i++;
		}
		final BytecodeProvider bytecodeProvider = creationContext.getServiceRegistry().getService( BytecodeProvider.class );

		return bytecodeProvider.getReflectionOptimizer(
				bootDescriptor.getComponentClass(),
				propertyAccessMap
		);
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public EmbeddableInstantiator getInstantiator() {
		return instantiator;
	}
}
