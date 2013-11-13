/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.id.enhanced;

import java.io.Serializable;

import org.hibernate.id.IntegralDataTypeHolder;

/**
 * An optimizer that performs no optimization.  The database is hit for
 * every request.
 *
 * @deprecated This is the fallback Optimizer chosen when we fail to instantiate one
 * of the proper implementations. Using this implementation is probably a performance
 * problem.
 */
@Deprecated
public final class NoopOptimizer extends AbstractOptimizer {
	private volatile IntegralDataTypeHolder lastSourceValue;

	/**
	 * Constructs a NoopOptimizer
	 *
	 * @param returnClass The Java type of the values to be generated
	 * @param incrementSize The increment size.
	 */
	public NoopOptimizer(Class returnClass, int incrementSize) {
		super( returnClass, incrementSize );
	}

	@Override
	public synchronized Serializable generate(AccessCallback callback) {
		// IMPL NOTE : it is incredibly important that the method-local variable be used here to
		//		avoid concurrency issues.
		IntegralDataTypeHolder value = null;
		while ( value == null || value.lt( 1 ) ) {
			value = callback.getNextValue();
		}
		lastSourceValue = value;
		return value.makeValue();
	}

	@Override
	public IntegralDataTypeHolder getLastSourceValue() {
		return lastSourceValue;
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		return false;
	}
}
