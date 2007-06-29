package org.hibernate.bytecode.javassist;

import org.hibernate.bytecode.ReflectionOptimizer;

import java.io.Serializable;

/**
 * ReflectionOptimizer implementation for Javassist.
 *
 * @author Steve Ebersole
 */
public class ReflectionOptimizerImpl implements ReflectionOptimizer, Serializable {

	private final InstantiationOptimizer instantiationOptimizer;
	private final AccessOptimizer accessOptimizer;

	public ReflectionOptimizerImpl(
			InstantiationOptimizer instantiationOptimizer,
	        AccessOptimizer accessOptimizer) {
		this.instantiationOptimizer = instantiationOptimizer;
		this.accessOptimizer = accessOptimizer;
	}

	public InstantiationOptimizer getInstantiationOptimizer() {
		return instantiationOptimizer;
	}

	public AccessOptimizer getAccessOptimizer() {
		return accessOptimizer;
	}

}
