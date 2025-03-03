/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo.bytebuddy;

import static org.hibernate.internal.CoreLogging.messageLogger;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.pool.TypePool;

public class ByteBuddyProxyHelper implements Serializable {

	private static final CoreMessageLogger LOG = messageLogger( ByteBuddyProxyHelper.class );
	private static final String PROXY_NAMING_SUFFIX = "HibernateProxy";
	private static final TypeDescription OBJECT = TypeDescription.ForLoadedType.of(Object.class);

	private final ByteBuddyState byteBuddyState;

	public ByteBuddyProxyHelper(ByteBuddyState byteBuddyState) {
		this.byteBuddyState = byteBuddyState;
	}

	@SuppressWarnings("rawtypes")
	public Class buildProxy(
			final Class<?> persistentClass,
			final Class<?>[] interfaces) {
		Set<Class<?>> key = new HashSet<>();
		if ( interfaces.length == 1 ) {
			key.add( persistentClass );
		}
		Collections.addAll( key, interfaces );

		final String proxyClassName = persistentClass.getTypeName() + "$" + PROXY_NAMING_SUFFIX;
		return byteBuddyState.loadProxy( persistentClass, proxyClassName,
				proxyBuilder( TypeDescription.ForLoadedType.of( persistentClass ), new TypeList.Generic.ForLoadedTypes( interfaces ) ) );
	}

	/**
	 * @deprecated Use {@link #buildUnloadedProxy(TypePool, TypeDefinition, Collection)} instead.
	 */
	@Deprecated
	public DynamicType.Unloaded<?> buildUnloadedProxy(final Class<?> persistentClass, final Class<?>[] interfaces) {
		return byteBuddyState.make( proxyBuilderLegacy( TypeDescription.ForLoadedType.of( persistentClass ),
				new TypeList.Generic.ForLoadedTypes( interfaces ) ) );
	}

	/**
	 * Do not remove: used by Quarkus
	 */
	public DynamicType.Unloaded<?> buildUnloadedProxy(TypePool typePool, TypeDefinition persistentClass,
			Collection<? extends TypeDefinition> interfaces) {
		return byteBuddyState.make( typePool, proxyBuilderLegacy( persistentClass, interfaces ) );
	}

	private Function<ByteBuddy, DynamicType.Builder<?>> proxyBuilderLegacy(TypeDefinition persistentClass,
			Collection<? extends TypeDefinition> interfaces) {
		final BiFunction<ByteBuddy, NamingStrategy, DynamicType.Builder<?>> proxyBuilder =
				proxyBuilder( persistentClass, interfaces );
		final NamingStrategy.Suffixing namingStrategy =
				new NamingStrategy.Suffixing( PROXY_NAMING_SUFFIX, new NamingStrategy.Suffixing.BaseNameResolver.ForFixedValue( persistentClass.getTypeName() ) );
		return byteBuddy -> proxyBuilder.apply( byteBuddy, namingStrategy );
	}

	private BiFunction<ByteBuddy, NamingStrategy, DynamicType.Builder<?>> proxyBuilder(TypeDefinition persistentClass,
			Collection<? extends TypeDefinition> interfaces) {
		ByteBuddyState.ProxyDefinitionHelpers helpers = byteBuddyState.getProxyDefinitionHelpers();
		return (byteBuddy, namingStrategy) -> helpers.appendIgnoreAlsoAtEnd( byteBuddy
				.ignore( helpers.getGroovyGetMetaClassFilter() )
				.with( namingStrategy )
				.subclass( interfaces.size() == 1 ? persistentClass : OBJECT, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING )
				.implement( interfaces )
				.method( helpers.getVirtualNotFinalizerFilter() )
						.intercept( helpers.getDelegateToInterceptorDispatcherMethodDelegation() )
				.method( helpers.getProxyNonInterceptedMethodFilter() )
						.intercept( SuperMethodCall.INSTANCE )
				.defineField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE )
				.implement( ProxyConfiguration.class )
						.intercept( helpers.getInterceptorFieldAccessor() )
		);
	}

	public HibernateProxy deserializeProxy(SerializableProxy serializableProxy) {
		final ByteBuddyInterceptor interceptor = new ByteBuddyInterceptor(
				serializableProxy.getEntityName(),
				serializableProxy.getPersistentClass(),
				serializableProxy.getInterfaces(),
				serializableProxy.getId(),
				resolveIdGetterMethod( serializableProxy ),
				resolveIdSetterMethod( serializableProxy ),
				serializableProxy.getComponentIdType(),
				null,
				ReflectHelper.overridesEquals( serializableProxy.getPersistentClass() )
		);

		// note: interface is assumed to already contain HibernateProxy.class
		try {
			final Class<?> proxyClass = buildProxy(
					serializableProxy.getPersistentClass(),
					serializableProxy.getInterfaces()
			);
			PrimeAmongSecondarySupertypes instance = (PrimeAmongSecondarySupertypes) proxyClass.getDeclaredConstructor().newInstance();
			final ProxyConfiguration proxyConfiguration = instance.asProxyConfiguration();
			if ( proxyConfiguration == null ) {
				throw new HibernateException( "Produced proxy does not correctly implement ProxyConfiguration" );
			}
			proxyConfiguration.$$_hibernate_set_interceptor( interceptor );
			final HibernateProxy hibernateProxy = instance.asHibernateProxy();
			if ( hibernateProxy == null ) {
				throw new HibernateException( "Produced proxy does not correctly implement HibernateProxy" );
			}
			return hibernateProxy;
		}
		catch (Throwable t) {
			final String message = LOG.bytecodeEnhancementFailed( serializableProxy.getEntityName() );
			LOG.error( message, t );
			throw new HibernateException( message, t );
		}
	}

	@SuppressWarnings("unchecked")
	private static Method resolveIdGetterMethod(SerializableProxy serializableProxy) {
		if ( serializableProxy.getIdentifierGetterMethodName() == null ) {
			return null;
		}

		try {
			return serializableProxy.getIdentifierGetterMethodClass().getDeclaredMethod( serializableProxy.getIdentifierGetterMethodName() );
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"Unable to deserialize proxy [%s, %s]; could not locate id getter method [%s] on entity class [%s]",
							serializableProxy.getEntityName(),
							serializableProxy.getId(),
							serializableProxy.getIdentifierGetterMethodName(),
							serializableProxy.getIdentifierGetterMethodClass()
					)
			);
		}
	}

	@SuppressWarnings("unchecked")
	private static Method resolveIdSetterMethod(SerializableProxy serializableProxy) {
		if ( serializableProxy.getIdentifierSetterMethodName() == null ) {
			return null;
		}

		try {
			return serializableProxy.getIdentifierSetterMethodClass().getDeclaredMethod(
					serializableProxy.getIdentifierSetterMethodName(),
					serializableProxy.getIdentifierSetterMethodParams()
			);
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"Unable to deserialize proxy [%s, %s]; could not locate id setter method [%s] on entity class [%s]",
							serializableProxy.getEntityName(),
							serializableProxy.getId(),
							serializableProxy.getIdentifierSetterMethodName(),
							serializableProxy.getIdentifierSetterMethodClass()
					)
			);
		}
	}
}
