/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2013, Red Hat Inc. or third-party contributors as
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
