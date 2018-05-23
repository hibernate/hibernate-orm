/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.pojo.bytebuddy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

import net.bytebuddy.NamingStrategy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import static net.bytebuddy.matcher.ElementMatchers.isFinalizer;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static org.hibernate.internal.CoreLogging.messageLogger;

public class ByteBuddyProxyFactory implements ProxyFactory, Serializable {

	private static final CoreMessageLogger LOG = messageLogger( ByteBuddyProxyFactory.class );
	private static final String PROXY_NAMING_SUFFIX = Environment.useLegacyProxyClassnames() ? "HibernateProxy$" : "HibernateProxy";

	private Class persistentClass;
	private String entityName;
	private Class[] interfaces;
	private Method getIdentifierMethod;
	private Method setIdentifierMethod;
	private CompositeType componentIdType;
	private boolean overridesEquals;

	private Class proxyClass;

	@Override
	public void postInstantiate(
			String entityName,
			Class persistentClass,
			Set<Class> interfaces,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType) throws HibernateException {
		this.entityName = entityName;
		this.persistentClass = persistentClass;
		this.interfaces = toArray( interfaces );
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		this.overridesEquals = ReflectHelper.overridesEquals( persistentClass );

		this.proxyClass = buildProxy( persistentClass, this.interfaces );
	}

	private Class[] toArray(Set<Class> interfaces) {
		if ( interfaces == null ) {
			return ArrayHelper.EMPTY_CLASS_ARRAY;
		}

		return interfaces.toArray( new Class[interfaces.size()] );
	}

	public static Class buildProxy(
			final Class persistentClass,
			final Class[] interfaces) {
		Set<Class<?>> key = new HashSet<Class<?>>();
		if ( interfaces.length == 1 ) {
			key.add( persistentClass );
		}
		key.addAll( Arrays.<Class<?>>asList( interfaces ) );

		final TypeCache<TypeCache.SimpleKey> cacheForProxies = ByteBuddyState.getCacheForProxies();

		return cacheForProxies.findOrInsert( persistentClass.getClassLoader(), new TypeCache.SimpleKey(key), () ->
			ByteBuddyState.getStaticByteBuddyInstance()
			.ignore( isSynthetic().and( named( "getMetaClass" ).and( returns( td -> "groovy.lang.MetaClass".equals( td.getName() ) ) ) ) )
			.with( new NamingStrategy.SuffixingRandom( PROXY_NAMING_SUFFIX, new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( persistentClass.getName() ) ) )
			.subclass( interfaces.length == 1 ? persistentClass : Object.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING )
			.implement( (Type[]) interfaces )
			.method( isVirtual().and( not( isFinalizer() ) ) )
			.intercept( MethodDelegation.to( ProxyConfiguration.InterceptorDispatcher.class ) )
			.method( nameStartsWith( "$$_hibernate_" ).and( isVirtual() ) )
			.intercept( SuperMethodCall.INSTANCE )
			.defineField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE )
			.implement( ProxyConfiguration.class )
			.intercept( FieldAccessor.ofField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME ).withAssigner( Assigner.DEFAULT, Assigner.Typing.DYNAMIC ) )
			.make()
			.load( persistentClass.getClassLoader(), ByteBuddyState.resolveClassLoadingStrategy( persistentClass ) )
			.getLoaded(), cacheForProxies );
	}

	@Override
	public HibernateProxy getProxy(
			Serializable id,
			SharedSessionContractImplementor session) throws HibernateException {
		final ByteBuddyInterceptor interceptor = new ByteBuddyInterceptor(
				entityName,
				persistentClass,
				interfaces,
				id,
				getIdentifierMethod,
				setIdentifierMethod,
				componentIdType,
				session,
				overridesEquals
		);

		try {
			final HibernateProxy proxy = (HibernateProxy) proxyClass.newInstance();
			( (ProxyConfiguration) proxy ).$$_hibernate_set_interceptor( interceptor );

			return proxy;
		}
		catch (Throwable t) {
			LOG.error( LOG.bytecodeEnhancementFailed( entityName ), t );
			throw new HibernateException( LOG.bytecodeEnhancementFailed( entityName ), t );
		}
	}

	public static HibernateProxy deserializeProxy(SerializableProxy serializableProxy) {
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
			final Class proxyClass = buildProxy(
					serializableProxy.getPersistentClass(),
					serializableProxy.getInterfaces()
			);
			final HibernateProxy proxy = (HibernateProxy) proxyClass.newInstance();
			( (ProxyConfiguration) proxy ).$$_hibernate_set_interceptor( interceptor );
			return proxy;
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
