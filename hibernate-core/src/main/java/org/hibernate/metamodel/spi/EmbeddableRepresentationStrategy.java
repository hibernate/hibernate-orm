/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;
import org.hibernate.bytecode.spi.ReflectionOptimizer;

/**
 * Describes the representation of a particular embeddable type.
 *
 * @todo (6.x) add `@EmbeddableRepresentationStrategy` - see https://hibernate.atlassian.net/browse/HHH-14951
 */
@Incubating
public interface EmbeddableRepresentationStrategy extends ManagedTypeRepresentationStrategy {
	/**
	 * Create a delegate capable of instantiating instances of the represented type.
	 */
	EmbeddableInstantiator getInstantiator();

	default EmbeddableInstantiator getInstantiatorForDiscriminator(Object discriminatorValue) {
		return getInstantiator();
	}

	default EmbeddableInstantiator getInstantiatorForClass(String className) {
		return getInstantiator();
	}

	/**
	 * The reflection optimizer to use for this embeddable.
	 *
	 * https://hibernate.atlassian.net/browse/HHH-14952
	 */
	ReflectionOptimizer getReflectionOptimizer();
}
