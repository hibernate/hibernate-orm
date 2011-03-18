/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

	public InstantiationOptimizerAdapter(FastClass fastClass) {
		this.fastClass = fastClass;
	}

	public Object newInstance() {
		try {
			return fastClass.newInstance();
		}
		catch ( Throwable t ) {
			throw new InstantiationException(
					"Could not instantiate entity with Javassist optimizer: ",
			        fastClass.getJavaClass(), t
			);
		}
	}
}
