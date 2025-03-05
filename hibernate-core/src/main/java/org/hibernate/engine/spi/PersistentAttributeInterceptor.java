/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.util.Collections;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.InterceptorImplementor;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;

/**
 * The base contract for interceptors that can be injected into
 * enhanced entities for the purpose of intercepting attribute access
 *
 * @author Steve Ebersole
 *
 * @see PersistentAttributeInterceptable
 */
@Incubating
@SuppressWarnings("unused")
public interface PersistentAttributeInterceptor extends InterceptorImplementor {
	boolean readBoolean(Object obj, String name, boolean oldValue);

	boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue);

	byte readByte(Object obj, String name, byte oldValue);

	byte writeByte(Object obj, String name, byte oldValue, byte newValue);

	char readChar(Object obj, String name, char oldValue);

	char writeChar(Object obj, String name, char oldValue, char newValue);

	short readShort(Object obj, String name, short oldValue);

	short writeShort(Object obj, String name, short oldValue, short newValue);

	int readInt(Object obj, String name, int oldValue);

	int writeInt(Object obj, String name, int oldValue, int newValue);

	float readFloat(Object obj, String name, float oldValue);

	float writeFloat(Object obj, String name, float oldValue, float newValue);

	double readDouble(Object obj, String name, double oldValue);

	double writeDouble(Object obj, String name, double oldValue, double newValue);

	long readLong(Object obj, String name, long oldValue);

	long writeLong(Object obj, String name, long oldValue, long newValue);

	Object readObject(Object obj, String name, Object oldValue);

	Object writeObject(Object obj, String name, Object oldValue, Object newValue);

	/**
	 * @deprecated Just as the method it overrides.  Interceptors that deal with
	 * lazy state should implement {@link BytecodeLazyAttributeInterceptor}
	 */
	@Deprecated
	@Override
	default Set<String> getInitializedLazyAttributeNames() {
		return Collections.emptySet();
	}

	/**
	 * @deprecated Just as the method it overrides.  Interceptors that deal with
	 * lazy state should implement {@link BytecodeLazyAttributeInterceptor}
	 */
	@Override
	@Deprecated
	default void attributeInitialized(String name) {
	}

	/**
	 *
	 * Callback from the enhanced class that an attribute has been loaded
	 *
	 * @deprecated Interceptors that deal with
	 * 	 * lazy state should implement {@link BytecodeLazyAttributeInterceptor}
	 *
	 * @param fieldName
	 * @return true id the attribute is loaded false otherwise
	 */
	@Deprecated
	default boolean isAttributeLoaded(String fieldName){
		return false;
	}
}
