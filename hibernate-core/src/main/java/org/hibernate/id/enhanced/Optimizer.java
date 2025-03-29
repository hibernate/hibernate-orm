/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;

import org.hibernate.id.IntegralDataTypeHolder;

/**
 * Performs optimization on an optimizable identifier generator.  Typically
 * this optimization takes the form of trying to ensure we do not have to
 * hit the database on each and every request to get an identifier value.
 * <p>
 * Optimizers work on constructor injection.  They should provide
 * a constructor with the following arguments <ol>
 * <li>java.lang.Class - The return type for the generated values</li>
 * <li>int - The increment size</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public interface Optimizer {
	/**
	 * Generate an identifier value accounting for this specific optimization.
	 *
	 * @implNote All known implementors are synchronized. Consider carefully
	 *           if a new implementation could drop this requirement.
	 *
	 * @param callback Callback to access the underlying value source.
	 * @return The generated identifier value.
	 */
	Serializable generate(AccessCallback callback);

	/**
	 * A common means to access the last value obtained from the underlying
	 * source.  This is intended for testing purposes, since accessing the
	 * underlying database source directly is much more difficult.
	 *
	 * @return The last value we obtained from the underlying source;
	 * null indicates we have not yet consulted with the source.
	 */
	IntegralDataTypeHolder getLastSourceValue();

	/**
	 * Retrieves the defined increment size.
	 *
	 * @return The increment size.
	 */
	int getIncrementSize();

	/**
	 * Are increments to be applied to the values stored in the underlying
	 * value source?
	 *
	 * @return True if the values in the source are to be incremented
	 * according to the defined increment size; false otherwise, in which
	 * case the increment is totally an in memory construct.
	 */
	boolean applyIncrementSizeToSourceValues();
}
