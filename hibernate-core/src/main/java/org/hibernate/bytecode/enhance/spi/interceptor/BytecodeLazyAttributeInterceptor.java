/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Set;

import org.hibernate.Incubating;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface BytecodeLazyAttributeInterceptor extends SessionAssociableInterceptor {
	/**
	 * The name of the entity this interceptor is meant to intercept
	 */
	String getEntityName();

	/**
	 * The id of the entity instance this interceptor is associated with
	 */
	Object getIdentifier();

	/**
	 * The names of all lazy attributes which have been initialized
	 */
	@Override
	Set<String> getInitializedLazyAttributeNames();

	/**
	 * Callback from the enhanced class that an attribute has been read or written
	 */
	void attributeInitialized(String name);

	@Override
	boolean isAttributeLoaded(String fieldName);

	boolean hasAnyUninitializedAttributes();

}
