/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.io.Serializable;

import org.hibernate.bytecode.spi.ReflectionOptimizer;

/**
 * ReflectionOptimizer implementation for Javassist.
 *
 * @author Steve Ebersole
 */
public class ReflectionOptimizerImpl implements ReflectionOptimizer, Serializable {
	private final InstantiationOptimizer instantiationOptimizer;
	private final AccessOptimizer accessOptimizer;

	/**
	 * Constructs a ReflectionOptimizerImpl
	 *
	 * @param instantiationOptimizer The instantiation optimizer to use
	 * @param accessOptimizer The property access optimizer to use.
	 */
	public ReflectionOptimizerImpl(
			InstantiationOptimizer instantiationOptimizer,
			AccessOptimizer accessOptimizer) {
		this.instantiationOptimizer = instantiationOptimizer;
		this.accessOptimizer = accessOptimizer;
	}

	@Override
	public InstantiationOptimizer getInstantiationOptimizer() {
		return instantiationOptimizer;
	}

	@Override
	public AccessOptimizer getAccessOptimizer() {
		return accessOptimizer;
	}

}
