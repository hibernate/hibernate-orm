/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import org.hibernate.proxy.ProxyFactory;

/**
 * Specialization of ManagedTypeRepresentationStrategy for entity types
 * adding the ability to generate a proxy factory
 *
 * @author Steve Ebersole
 */
public interface EntityRepresentationStrategy extends ManagedTypeRepresentationStrategy {
	/**
	 * Create the delegate capable of producing proxies for the given entity
	 */
	ProxyFactory generateProxyFactory();
}
