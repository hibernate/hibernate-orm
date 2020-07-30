/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.io.Serializable;

import org.hibernate.InstantiationException;
import org.hibernate.bytecode.spi.ReflectionOptimizer;

/**
 * The {@link org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer} implementation for Javassist
 * which simply acts as an adapter to the {@link FastClass} class.
 *
 * @author Steve Ebersole
 */
public class InstantiationOptimizerAdapter implements ReflectionOptimizer.InstantiationOptimizer, Serializable {
	private final FastClass fastClass;

	/**
	 * Constructs the InstantiationOptimizerAdapter
	 *
	 * @param fastClass The fast class for the class to be instantiated here.
	 */
	public InstantiationOptimizerAdapter(FastClass fastClass) {
		this.fastClass = fastClass;
	}

	@Override
	public Object newInstance() {
		try {
			return fastClass.newInstance();
		}
		catch ( Exception e ) {
			throw new InstantiationException(
					"Could not instantiate entity with Javassist optimizer: ",
					fastClass.getJavaClass(),
					e
			);
		}
	}
}
