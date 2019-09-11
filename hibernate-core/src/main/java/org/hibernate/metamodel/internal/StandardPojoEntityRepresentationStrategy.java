/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.Instantiator;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessBasicImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyEmbeddedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyIndexBackRefImpl;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;

/**
 * @author Steve Ebersole
 */
public class StandardPojoEntityRepresentationStrategy implements EntityRepresentationStrategy {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StandardPojoEntityRepresentationStrategy.class );

	private final JavaTypeDescriptor<?> mappedJtd;
	private final JavaTypeDescriptor<?> proxyJtd;

	private final boolean isBytecodeEnhanced;
	private final boolean lifecycleImplementor;

	private final ReflectionOptimizer reflectionOptimizer;
	private final ProxyFactory proxyFactory;
	private final Instantiator instantiator;

	private final StrategySelector strategySelector;

	private final String identifierPropertyName;
	private final PropertyAccess identifierPropertyAccess;
	private final Map<String, PropertyAccess> propertyAccessMap = new ConcurrentHashMap<>();

	public StandardPojoEntityRepresentationStrategy(
			PersistentClass bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final JavaTypeDescriptorRegistry jtdRegistry = sessionFactory.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry();

		final Class<?> mappedJavaType = bootDescriptor.getMappedClass();
		this.mappedJtd = jtdRegistry.getDescriptor( mappedJavaType );

		final Class<?> proxyJavaType = bootDescriptor.getProxyInterface();
		this.proxyJtd = jtdRegistry.getDescriptor( proxyJavaType );

		this.lifecycleImplementor = Lifecycle.class.isAssignableFrom( mappedJavaType );
		this.isBytecodeEnhanced = PersistentAttributeInterceptable.class.isAssignableFrom( mappedJavaType );


		final Property identifierProperty = bootDescriptor.getIdentifierProperty();
		if ( identifierProperty == null ) {
			identifierPropertyName = null;
			identifierPropertyAccess = PropertyAccessStrategyEmbeddedImpl.INSTANCE.buildPropertyAccess(
					proxyJtd != null ? proxyJtd.getJavaType() : mappedJtd.getJavaType(),
					"id"
			);
		}
		else {
			identifierPropertyName = identifierProperty.getName();
			identifierPropertyAccess = makePropertyAccess( identifierProperty );
		}

//		final BytecodeProvider bytecodeProvider = creationContext.getBootstrapContext().getBytecodeProvider();
		final BytecodeProvider bytecodeProvider = Environment.getBytecodeProvider();

		this.proxyFactory = createProxyFactory( bootDescriptor, bytecodeProvider, creationContext );

		this.reflectionOptimizer = resolveReflectionOptimizer( bootDescriptor, bytecodeProvider );

		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			this.instantiator = new OptimizedPojoInstantiatorImpl<>( mappedJtd, reflectionOptimizer );
		}
		else {
			this.instantiator = new PojoInstantiatorImpl<>( mappedJtd );
		}

		this.strategySelector = sessionFactory.getServiceRegistry().getService( StrategySelector.class );
	}

	private PropertyAccess resolveIdentifierPropertyAccess(PersistentClass bootDescriptor) {
		final Property identifierProperty = bootDescriptor.getIdentifierProperty();

		if ( identifierProperty == null ) {
			return PropertyAccessStrategyEmbeddedImpl.INSTANCE.buildPropertyAccess(
					proxyJtd != null ? proxyJtd.getJavaType() : mappedJtd.getJavaType(),
					"id"
			);
		}

		return makePropertyAccess( identifierProperty );
	}

	private ProxyFactory createProxyFactory(
			PersistentClass bootDescriptor,
			BytecodeProvider bytecodeProvider,
			RuntimeModelCreationContext creationContext) {
		/*
		 * We need to preserve the order of the interfaces they were put into the set, since javassist will choose the
		 * first one's class-loader to construct the proxy class with. This is also the reason why HibernateProxy.class
		 * should be the last one in the order (on JBossAS7 its class-loader will be org.hibernate module's class-
		 * loader, which will not see the classes inside deployed apps.  See HHH-3078
		 */
		final Set<Class> proxyInterfaces = new java.util.LinkedHashSet<>();

		final Class mappedClass = mappedJtd.getJavaType();
		final Class proxyInterface = proxyJtd.getJavaType();

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

		//noinspection unchecked
		final Iterator<Subclass> subclasses = bootDescriptor.getSubclassIterator();
		while ( subclasses.hasNext() ) {
			final Subclass subclass = subclasses.next();
			final Class subclassProxy = subclass.getProxyInterface();
			final Class subclassClass = subclass.getMappedClass();
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

		Iterator properties = bootDescriptor.getPropertyIterator();
		Class clazz = bootDescriptor.getMappedClass();
		while ( properties.hasNext() ) {
			Property property = (Property) properties.next();
			Method method = property.getGetter( clazz ).getMethod();
			if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
				LOG.gettersOfLazyClassesCannotBeFinal( bootDescriptor.getEntityName(), property.getName() );
			}
			method = property.getSetter( clazz ).getMethod();
			if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
				LOG.settersOfLazyClassesCannotBeFinal( bootDescriptor.getEntityName(), property.getName() );
			}
		}

		final Method idGetterMethod = identifierPropertyAccess == null ? null : identifierPropertyAccess.getGetter().getMethod();
		final Method idSetterMethod = identifierPropertyAccess == null ? null : identifierPropertyAccess.getSetter().getMethod();

		final Method proxyGetIdentifierMethod = idGetterMethod == null || proxyInterface == null
				? null
				: ReflectHelper.getMethod( proxyInterface, idGetterMethod );
		final Method proxySetIdentifierMethod = idSetterMethod == null || proxyInterface == null
				? null
				: ReflectHelper.getMethod( proxyInterface, idSetterMethod );

		ProxyFactory pf = bytecodeProvider.getProxyFactoryFactory().buildProxyFactory( creationContext.getSessionFactory() );
		try {
			pf.postInstantiate(
					bootDescriptor.getEntityName(),
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					bootDescriptor.hasEmbeddedIdentifier() ?
							(CompositeType) bootDescriptor.getIdentifier().getType() :
							null
			);

			return pf;
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( bootDescriptor.getEntityName(), he );
			return null;
		}
	}

	private ReflectionOptimizer resolveReflectionOptimizer(
			PersistentClass bootType,
			BytecodeProvider bytecodeProvider) {
		final Class javaTypeToReflect;
		if ( proxyFactory != null ) {
			assert proxyJtd != null;
			javaTypeToReflect = proxyJtd.getJavaType();
		}
		else {
			javaTypeToReflect = mappedJtd.getJavaType();
		}

		final List<String> getterNames = new ArrayList<>();
		final List<String> setterNames = new ArrayList<>();
		final List<Class> getterTypes = new ArrayList<>();

		boolean foundCustomAccessor = false;

		//noinspection unchecked
		final Iterator<Property> itr = bootType.getPropertyClosureIterator();
		int i = 0;
		while ( itr.hasNext() ) {
			//TODO: redesign how PropertyAccessors are acquired...
			final Property property = itr.next();
			final PropertyAccess propertyAccess = makePropertyAccess( property );

			propertyAccessMap.put( property.getName(), propertyAccess );

			if ( ! (propertyAccess instanceof PropertyAccessBasicImpl) ) {
				foundCustomAccessor = true;
			}

			getterNames.add( propertyAccess.getGetter().getMethodName() );
			getterTypes.add( propertyAccess.getGetter().getReturnType() );

			setterNames.add( propertyAccess.getSetter().getMethodName() );

			i++;
		}

		if ( foundCustomAccessor || ! Environment.useReflectionOptimizer() ) {
			return null;
		}

		return bytecodeProvider.getReflectionOptimizer(
				javaTypeToReflect,
				getterNames.toArray( new String[0] ),
				setterNames.toArray( new String[0] ),
				getterTypes.toArray( new Class[0] )
		);
	}

	private PropertyAccess makePropertyAccess(Property bootAttributeDescriptor) {
		PropertyAccessStrategy strategy = null;

		final String propertyAccessorName = bootAttributeDescriptor.getPropertyAccessorName();
		final BuiltInPropertyAccessStrategies namedStrategy = BuiltInPropertyAccessStrategies.interpret(
				propertyAccessorName );

		if ( namedStrategy != null ) {
			strategy = namedStrategy.getStrategy();
		}

		if ( strategy == null ) {
			if ( StringHelper.isNotEmpty( propertyAccessorName ) ) {
				// handle explicitly specified attribute accessor
				strategy = strategySelector.resolveStrategy( PropertyAccessStrategy.class, propertyAccessorName );
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
							mappedJtd.getJavaType().getName(),
							bootAttributeDescriptor.getName()
					)
			);
		}

		return strategy.buildPropertyAccess( mappedJtd.getJavaType(), bootAttributeDescriptor.getName() );
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
	public Instantiator getInstantiator() {
		return instantiator;
	}

	@Override
	public ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	@Override
	public boolean isLifecycleImplementor() {
		return lifecycleImplementor;
	}

	@Override
	public boolean isBytecodeEnhanced() {
		return isBytecodeEnhanced;
	}

	@Override
	public JavaTypeDescriptor<?> getMappedJavaTypeDescriptor() {
		return mappedJtd;
	}

	@Override
	public JavaTypeDescriptor<?> getProxyJavaTypeDescriptor() {
		return proxyJtd;
	}

	@Override
	public PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor) {
		if ( bootAttributeDescriptor.getName().equals( identifierPropertyName ) ) {
			return identifierPropertyAccess;
		}

		return propertyAccessMap.get( bootAttributeDescriptor.getName() );
	}
}
