/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import java.util.function.Consumer;

import org.hibernate.EntityNameResolver;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Specialization of ManagedTypeRepresentationStrategy for an entity type
 * adding the ability to generate an instantiator and a proxy factory
 *
 * @author Steve Ebersole
 */
public interface EntityRepresentationStrategy extends ManagedTypeRepresentationStrategy {
	/**
	 * Create a delegate capable of instantiating instances of the represented type.
	 */
	Instantiator<?> getInstantiator();

	/**
	 * Create the delegate capable of producing proxies for the given entity
	 */
	ProxyFactory getProxyFactory();

	default boolean isLifecycleImplementor() {
		return false;
	}

	default boolean isBytecodeEnhanced() {
		return false;
	}

	/**
	 * The Java type descriptor for the concrete entity type
	 */
	JavaTypeDescriptor<?> getMappedJavaTypeDescriptor();

	JavaTypeDescriptor<?> getProxyJavaTypeDescriptor();

	/**
	 * The Java type descriptor for the type returned when the entity is loaded
	 */
	default JavaTypeDescriptor<?> getLoadJavaTypeDescriptor() {
		return getMappedJavaTypeDescriptor();
	}

	default void visitEntityNameResolvers(Consumer<EntityNameResolver> consumer) {
		// byt default do nothing
	}
}
