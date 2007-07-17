package org.hibernate.bytecode.cglib;

import org.hibernate.bytecode.ReflectionOptimizer;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * ReflectionOptimizer implementation for CGLIB.
 *
 * @author Steve Ebersole
 */
public class ReflectionOptimizerImpl implements ReflectionOptimizer, Serializable {
	private transient InstantiationOptimizerAdapter instantiationOptimizer;
	private transient AccessOptimizerAdapter accessOptimizer;

	public ReflectionOptimizerImpl(
			InstantiationOptimizerAdapter instantiationOptimizer,
	        AccessOptimizerAdapter accessOptimizer) {
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
