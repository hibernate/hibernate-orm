/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;

import org.hibernate.id.IntegralDataTypeHolder;

/**
 * An optimizer that performs no optimization.  The database is hit for
 * every request.
 *
 * Using this implementation is probably not the most efficient choice.
 */
public final class NoopOptimizer extends AbstractOptimizer {
	private IntegralDataTypeHolder lastSourceValue;

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
	public Serializable generate(AccessCallback callback) {
		// IMPL NOTE : this method is called concurrently and is
		// not synchronized. It is very important to work on the
		// local variable: the field lastSourceValue is not
		// reliable as it might be mutated by multipled threads.
		// The lastSourceValue field is only accessed by tests,
		// so this is not a concern.
		IntegralDataTypeHolder value = callback.getNextValue();
		lastSourceValue = value;
		return value.makeValue();
	}

	@Override
	public IntegralDataTypeHolder getLastSourceValue() {
		return lastSourceValue;
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		// We allow the increment size to be 0 for backward-compatibility with legacy
		// ID generators; we don't apply a value of 0, so the default will be used instead.
		// We don't apply an increment size of 1, since it is already the default.
		return getIncrementSize() != 0 && getIncrementSize() != 1;
	}
}
