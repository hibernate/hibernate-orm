/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy.pojo.bytebuddy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImplConstants;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.pool.TypePool;

import static org.hibernate.internal.util.ReflectHelper.overridesEquals;

public class ByteBuddyProxyHelper implements Serializable {

	private static final String PROXY_NAMING_SUFFIX = "HibernateProxy";
	private static final TypeDescription OBJECT = TypeDescription.ForLoadedType.of(Object.class);

	private final ByteBuddyState byteBuddyState;
	private final EnhancerImplConstants constants;

	public ByteBuddyProxyHelper(ByteBuddyState byteBuddyState) {
		this.byteBuddyState = byteBuddyState;
		this.constants = byteBuddyState.getEnhancerConstants();
	}

	public Class<?> buildProxy(
			final Class<?> persistentClass,
			final Class<?>[] interfaces) {
		final String proxyClassName = persistentClass.getTypeName() + "$" + PROXY_NAMING_SUFFIX;
		return byteBuddyState.loadProxy( persistentClass, proxyClassName,
				proxyBuilder( TypeDescription.ForLoadedType.of( persistentClass ),
						new TypeList.Generic.ForLoadedTypes( interfaces ) ) );
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
	@SuppressWarnings("unused")
	public DynamicType.Unloaded<?> buildUnloadedProxy(TypePool typePool, TypeDefinition persistentClass,
			Collection<? extends TypeDefinition> interfaces) {
		return byteBuddyState.make( typePool, proxyBuilderLegacy( persistentClass, interfaces ) );
	}

	private Function<ByteBuddy, DynamicType.Builder<?>> proxyBuilderLegacy(TypeDefinition persistentClass,
			Collection<? extends TypeDefinition> interfaces) {
		final var proxyBuilder = proxyBuilder( persistentClass, interfaces );
		final var namingStrategy =
				new NamingStrategy.Suffixing( PROXY_NAMING_SUFFIX,
						new NamingStrategy.Suffixing.BaseNameResolver.ForFixedValue( persistentClass.getTypeName() ) );
		return byteBuddy -> proxyBuilder.apply( byteBuddy, namingStrategy );
	}

	private BiFunction<ByteBuddy, NamingStrategy, DynamicType.Builder<?>> proxyBuilder(TypeDefinition persistentClass,
			Collection<? extends TypeDefinition> interfaces) {
		var helpers = byteBuddyState.getProxyDefinitionHelpers();
		return (byteBuddy, namingStrategy) -> helpers.appendIgnoreAlsoAtEnd( byteBuddy
				.ignore( helpers.getGroovyGetMetaClassFilter() )
				.with( namingStrategy )
				.subclass( interfaces.size() == 1 ? persistentClass : OBJECT, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING )
				.implement( interfaces )
				.method( helpers.getVirtualNotFinalizerFilter() )
						.intercept( helpers.getDelegateToInterceptorDispatcherMethodDelegation() )
				.method( helpers.getProxyNonInterceptedMethodFilter() )
						.intercept( SuperMethodCall.INSTANCE )
				.defineField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, constants.modifierPRIVATE )
				.implement( constants.INTERFACES_for_ProxyConfiguration )
						.intercept( helpers.getInterceptorFieldAccessor() )
		);
	}

	public HibernateProxy deserializeProxy(SerializableProxy proxy) {
		final var interceptor = new ByteBuddyInterceptor(
				proxy.getEntityName(),
				proxy.getPersistentClass(),
				proxy.getInterfaces(),
				proxy.getId(),
				resolveIdGetterMethod( proxy ),
				resolveIdSetterMethod( proxy ),
				proxy.getComponentIdType(),
				null,
				overridesEquals( proxy.getPersistentClass() )
		);

		// note: interface is assumed to already contain HibernateProxy.class
		try {
			final var proxyClass = buildProxy( proxy.getPersistentClass(), proxy.getInterfaces() );
			final var instance = (PrimeAmongSecondarySupertypes) proxyClass.getDeclaredConstructor().newInstance();
			final var proxyConfiguration = instance.asProxyConfiguration();
			if ( proxyConfiguration == null ) {
				throw new HibernateException( "Produced proxy does not correctly implement ProxyConfiguration" );
			}
			proxyConfiguration.$$_hibernate_set_interceptor( interceptor );
			final var hibernateProxy = instance.asHibernateProxy();
			if ( hibernateProxy == null ) {
				throw new HibernateException( "Produced proxy does not correctly implement HibernateProxy" );
			}
			return hibernateProxy;
		}
		catch (Throwable t) {
			throw new HibernateException( "Bytecode enhancement failed for entity '"
					+ proxy.getEntityName() + "'", t );
		}
	}

	private static Method resolveIdGetterMethod(SerializableProxy serializableProxy) {
		if ( serializableProxy.getIdentifierGetterMethodName() == null ) {
			return null;
		}

		try {
			return serializableProxy.getIdentifierGetterMethodClass()
					.getDeclaredMethod( serializableProxy.getIdentifierGetterMethodName() );
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

	private static Method resolveIdSetterMethod(SerializableProxy serializableProxy) {
		if ( serializableProxy.getIdentifierSetterMethodName() == null ) {
			return null;
		}

		try {
			return serializableProxy.getIdentifierSetterMethodClass()
					.getDeclaredMethod( serializableProxy.getIdentifierSetterMethodName(),
							serializableProxy.getIdentifierSetterMethodParams() );
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
