/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.DetachedObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Gavin King
 * @since 7.2
 */
public class ProxyUtil {

	/**
	 * Get the entity instance underlying the given proxy, throwing
	 * an exception if the proxy is uninitialized. If the given
	 * object is not a proxy, simply return the argument.
	 */
	public static Object assertInitialized(Object maybeProxy) {
		final var lazyInitializer = extractLazyInitializer( maybeProxy );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.isUninitialized() ) {
				throw new PersistentObjectException( "Object was an uninitialized proxy for "
													+ lazyInitializer.getEntityName() );
			}
			//unwrap the object and return
			return lazyInitializer.getImplementation();
		}
		else {
			return maybeProxy;
		}
	}

	/**
	 * Get the entity instance underlying the given proxy, forcing
	 * initialization if the proxy is uninitialized. If the given
	 * object is not a proxy, simply return the argument.
	 * @throws DetachedObjectException if the given proxy does not
	 *         belong to the given session
	 */
	public static Object forceInitialize(Object maybeProxy, SharedSessionContractImplementor session) {
		final var lazyInitializer = extractLazyInitializer( maybeProxy );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.getSession() != session ) {
				throw new DetachedObjectException( "Given proxy does not belong to this persistence context" );
			}
			//initialize + unwrap the object and return it
			return lazyInitializer.getImplementation();
		}
		else if ( isPersistentAttributeInterceptable( maybeProxy ) ) {
			final var interceptor =
					asPersistentAttributeInterceptable( maybeProxy )
							.$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor lazinessInterceptor ) {
				if ( lazinessInterceptor.getLinkedSession() != session ) {
					throw new DetachedObjectException( "Given proxy does not belong to this persistence context" );
				}
				lazinessInterceptor.forceInitialize( maybeProxy, null );
			}
			return maybeProxy;
		}
		else {
			return maybeProxy;
		}
	}

	/**
	 * Determine of the given proxy is uninitialized. If the given
	 * object is not a proxy, simply return false.
	 * @throws DetachedObjectException if the given proxy does not
	 *         belong to the given session
	 */
	public static boolean isUninitialized(Object value, SharedSessionContractImplementor session) {
		// could be a proxy
		final var lazyInitializer = extractLazyInitializer( value );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.getSession() != session ) {
				throw new DetachedObjectException( "Given proxy does not belong to this persistence context" );
			}
			return lazyInitializer.isUninitialized();
		}
		// or an uninitialized enhanced entity ("bytecode proxy")
		else if ( isPersistentAttributeInterceptable( value ) ) {
			final var interceptor =
					(BytecodeLazyAttributeInterceptor)
							asPersistentAttributeInterceptable( value )
									.$$_hibernate_getInterceptor();
			if ( interceptor != null && interceptor.getLinkedSession() != session ) {
				throw new DetachedObjectException( "Given proxy does not belong to this persistence context" );
			}
			return interceptor instanceof EnhancementAsProxyLazinessInterceptor enhancementInterceptor
				&& !enhancementInterceptor.isInitialized();
		}
		else {
			return false;
		}
	}
}
