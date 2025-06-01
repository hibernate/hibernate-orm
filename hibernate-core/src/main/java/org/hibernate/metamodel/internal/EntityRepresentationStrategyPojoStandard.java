/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.CompositeTypeImplementor;

import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;
import static org.hibernate.internal.util.ReflectHelper.getMethod;
import static org.hibernate.metamodel.internal.PropertyAccessHelper.propertyAccessStrategy;
import static org.hibernate.proxy.pojo.ProxyFactoryHelper.validateGetterSetterMethodProxyability;

/**
 * @author Steve Ebersole
 */
public class EntityRepresentationStrategyPojoStandard implements EntityRepresentationStrategy {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityRepresentationStrategyPojoStandard.class );

	private final JavaType<?> mappedJtd;
	private final JavaType<?> proxyJtd;

	private final boolean isBytecodeEnhanced;

	private final ReflectionOptimizer reflectionOptimizer;
	private final ProxyFactory proxyFactory;
	private final EntityInstantiator instantiator;

	private final StrategySelector strategySelector;

	private final String identifierPropertyName;
	private final PropertyAccess identifierPropertyAccess;
	private final Map<String, PropertyAccess> propertyAccessMap;
	private final EmbeddableRepresentationStrategyPojo mapsIdRepresentationStrategy;

	public EntityRepresentationStrategyPojoStandard(
			PersistentClass bootDescriptor,
			EntityPersister runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
		final JavaTypeRegistry jtdRegistry = creationContext.getTypeConfiguration().getJavaTypeRegistry();

		final Class<?> mappedJavaType = bootDescriptor.getMappedClass();
		this.mappedJtd = jtdRegistry.resolveEntityTypeDescriptor( mappedJavaType );

		final Class<?> proxyJavaType = bootDescriptor.getProxyInterface();
		this.proxyJtd = proxyJavaType != null ? jtdRegistry.getDescriptor( proxyJavaType ) : null;

		this.isBytecodeEnhanced = isPersistentAttributeInterceptableType( mappedJavaType );

		final Property identifierProperty = bootDescriptor.getIdentifierProperty();
		if ( identifierProperty == null ) {
			identifierPropertyName = null;
			identifierPropertyAccess = null;

			if ( bootDescriptor.getIdentifier() instanceof Component descriptorIdentifierComponent ) {
				final Component identifierMapper = bootDescriptor.getIdentifierMapper();
				mapsIdRepresentationStrategy = new EmbeddableRepresentationStrategyPojo(
						identifierMapper == null ? descriptorIdentifierComponent : identifierMapper,
						() -> {
							final var type = (CompositeTypeImplementor) bootDescriptor.getIdentifierMapper().getType();
							return type.getMappingModelPart().getEmbeddableTypeDescriptor();
						},
						// we currently do not support custom instantiators for identifiers
						null,
						null,
						creationContext
				);
			}
			else {
				mapsIdRepresentationStrategy = null;
			}
		}
		else {
			mapsIdRepresentationStrategy = null;
			identifierPropertyName = identifierProperty.getName();
			identifierPropertyAccess = makePropertyAccess( identifierProperty );
		}

		this.strategySelector = creationContext.getServiceRegistry().getService( StrategySelector.class );

		final BytecodeProvider bytecodeProvider =
				creationContext.getBootstrapContext().getServiceRegistry()
						.requireService( BytecodeProvider.class );

		this.proxyFactory = resolveProxyFactory(
				bootDescriptor,
				runtimeDescriptor,
				proxyJtd,
				bytecodeProvider,
				creationContext
		);

		this.propertyAccessMap = buildPropertyAccessMap( bootDescriptor );
		this.reflectionOptimizer = resolveReflectionOptimizer( bytecodeProvider );

		this.instantiator = determineInstantiator( bootDescriptor, runtimeDescriptor.getEntityMetamodel() );
	}

	private ProxyFactory resolveProxyFactory(
			PersistentClass bootDescriptor,
			EntityPersister entityPersister,
			JavaType<?> proxyJavaType,
			BytecodeProvider bytecodeProvider,
			RuntimeModelCreationContext creationContext) {
		// todo : `@ConcreteProxy` handling
		if ( entityPersister.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()
				&& bootDescriptor.getRootClass() == bootDescriptor
				&& !bootDescriptor.hasSubclasses() ) {
			// the entity is bytecode enhanced for lazy loading
			// and is not part of an inheritance hierarchy,
			// so no need for a ProxyFactory
			return null;
		}
		else {
			final EntityMetamodel entityMetamodel = entityPersister.getEntityMetamodel();
			if ( proxyJavaType != null && entityMetamodel.isLazy() ) {
				final ProxyFactory proxyFactory =
						createProxyFactory( bootDescriptor, bytecodeProvider, creationContext );
				if ( proxyFactory == null ) {
					entityMetamodel.setLazy( false );
				}
				return proxyFactory;
			}
			else {
				return null;
			}
		}
	}

	private Map<String, PropertyAccess> buildPropertyAccessMap(PersistentClass bootDescriptor) {
		final Map<String, PropertyAccess> propertyAccessMap = new LinkedHashMap<>();
		for ( Property property : bootDescriptor.getPropertyClosure() ) {
			propertyAccessMap.put( property.getName(), makePropertyAccess( property ) );
		}
		return propertyAccessMap;
	}

	private EntityInstantiator determineInstantiator(PersistentClass bootDescriptor, EntityMetamodel entityMetamodel) {
		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			return new EntityInstantiatorPojoOptimized(
					entityMetamodel,
					bootDescriptor,
					mappedJtd,
					reflectionOptimizer.getInstantiationOptimizer()
			);
		}
		else {
			return new EntityInstantiatorPojoStandard( entityMetamodel, bootDescriptor, mappedJtd );
		}
	}

	private ProxyFactory createProxyFactory(
			PersistentClass bootDescriptor,
			BytecodeProvider bytecodeProvider,
			RuntimeModelCreationContext creationContext) {

		// HHH-17578 - We need to preserve the order of the interfaces to ensure
		// that the most general @Proxy declared interface at the top of a class
		// hierarchy will be used first when a HibernateProxy decides what it
		// should implement.
		final Set<Class<?>> proxyInterfaces = new LinkedHashSet<>();

		final Class<?> mappedClass = mappedJtd.getJavaTypeClass();
		final Class<?> proxyInterface = proxyJtd != null ? proxyJtd.getJavaTypeClass() : null;

		if ( proxyInterface != null && ! mappedClass.equals( proxyInterface ) ) {
			if ( ! proxyInterface.isInterface() ) {
				throw new MappingException(
						"proxy must be either an interface, or the class itself: " + bootDescriptor.getEntityName()
				);
			}
			proxyInterfaces.add( proxyInterface );
		}

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		for ( Subclass subclass : bootDescriptor.getSubclasses() ) {
			final Class<?> subclassProxy = subclass.getProxyInterface();
			final Class<?> subclassClass = subclass.getMappedClass();
			if ( subclassProxy != null && !subclassClass.equals( subclassProxy ) ) {
				if ( !subclassProxy.isInterface() ) {
					throw new MappingException(
							"proxy must be either an interface, or the class itself: " + subclass.getEntityName()
					);
				}
				proxyInterfaces.add( subclassProxy );
			}
		}

		proxyInterfaces.add( HibernateProxy.class );

		final Class<?> clazz = bootDescriptor.getMappedClass();
		final Method idGetterMethod;
		final Method idSetterMethod;
		try {
			for ( Property property : bootDescriptor.getProperties() ) {
				validateGetterSetterMethodProxyability( "Getter",
						property.getGetter( clazz ).getMethod() );
				validateGetterSetterMethodProxyability( "Setter",
						property.getSetter( clazz ).getMethod() );
			}
			if ( identifierPropertyAccess != null ) {
				idGetterMethod = identifierPropertyAccess.getGetter().getMethod();
				idSetterMethod = identifierPropertyAccess.getSetter().getMethod();
				validateGetterSetterMethodProxyability( "Getter", idGetterMethod );
				validateGetterSetterMethodProxyability( "Setter", idSetterMethod );
			}
			else {
				idGetterMethod = null;
				idSetterMethod = null;
			}
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( clazz.getName(), he );
			return null;
		}

		final Method proxyGetIdentifierMethod =
				idGetterMethod == null || proxyInterface == null ? null
						: getMethod( proxyInterface, idGetterMethod );
		final Method proxySetIdentifierMethod =
				idSetterMethod == null || proxyInterface == null ? null
						: getMethod( proxyInterface, idSetterMethod );

		final ProxyFactory proxyFactory =
				bytecodeProvider.getProxyFactoryFactory()
						.buildProxyFactory( creationContext.getSessionFactory() );
		try {
			proxyFactory.postInstantiate(
					bootDescriptor.getEntityName(),
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					bootDescriptor.hasEmbeddedIdentifier()
							? (CompositeType) bootDescriptor.getIdentifier().getType()
							: null
			);

			return proxyFactory;
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( bootDescriptor.getEntityName(), he );
			return null;
		}
	}

	private ReflectionOptimizer resolveReflectionOptimizer(BytecodeProvider bytecodeProvider) {
		return bytecodeProvider.getReflectionOptimizer(
				mappedJtd.getJavaTypeClass(),
				propertyAccessMap
		);
	}

	private PropertyAccess makePropertyAccess(Property bootAttributeDescriptor) {
		final Class<?> mappedClass = mappedJtd.getJavaTypeClass();
		final var strategy = propertyAccessStrategy( bootAttributeDescriptor, mappedClass, strategySelector );
		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							mappedJtd.getTypeName(),
							bootAttributeDescriptor.getName()
					)
			);
		}
		return strategy.buildPropertyAccess( mappedClass, bootAttributeDescriptor.getName(), true );
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return reflectionOptimizer;
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
	public boolean isBytecodeEnhanced() {
		return isBytecodeEnhanced;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return mappedJtd;
	}

	@Override
	public JavaType<?> getProxyJavaType() {
		return proxyJtd;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		if ( bootAttributeDescriptor.getName().equals( identifierPropertyName ) ) {
			return identifierPropertyAccess;
		}
		else {
			final PropertyAccess propertyAccess = propertyAccessMap.get( bootAttributeDescriptor.getName() );
			if ( propertyAccess != null ) {
				return propertyAccess;
			}
			else if ( mapsIdRepresentationStrategy != null ) {
				return mapsIdRepresentationStrategy.resolvePropertyAccess( bootAttributeDescriptor );
			}
			else {
				return null;
			}
		}
	}
}
