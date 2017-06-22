/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.bytecode.spi.ReflectionOptimizer;

/**
 * Factory (pluggable) for Initializer instances.
 *
 * @author Steve Ebersole
 */
public interface InstantiatorFactory {
	/**
	 *
	 * @param bootMapping
	 * @param runtimeDescriptor
	 * @param optimizer
	 * @return
	 */
	Instantiator createEmbeddableInstantiator(
			EmbeddedValueMapping bootMapping,
			EmbeddedTypeDescriptor runtimeDescriptor,
			ReflectionOptimizer.InstantiationOptimizer optimizer);

	Instantiator createEntityInstantiator(
			EntityMapping bootMapping,
			EntityDescriptor runtimeDescriptor,
			ReflectionOptimizer.InstantiationOptimizer optimizer);
}
