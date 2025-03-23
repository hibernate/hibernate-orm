/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import java.util.function.Consumer;

import org.hibernate.EntityNameResolver;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Specialization of {@link ManagedTypeRepresentationStrategy} for an entity type
 * adding the ability to generate an instantiator and a proxy factory
 *
 * @author Steve Ebersole
 */
public interface EntityRepresentationStrategy extends ManagedTypeRepresentationStrategy {
	/**
	 * Create a delegate capable of instantiating instances of the represented type.
	 */
	EntityInstantiator getInstantiator();

	/**
	 * Create the delegate capable of producing proxies for the given entity
	 */
	ProxyFactory getProxyFactory();

	default boolean isBytecodeEnhanced() {
		return false;
	}

	JavaType<?> getProxyJavaType();

	/**
	 * The Java type descriptor for the type returned when the entity is loaded
	 */
	default JavaType<?> getLoadJavaType() {
		return getMappedJavaType();
	}

	default void visitEntityNameResolvers(Consumer<EntityNameResolver> consumer) {
		// by default do nothing
	}
}
