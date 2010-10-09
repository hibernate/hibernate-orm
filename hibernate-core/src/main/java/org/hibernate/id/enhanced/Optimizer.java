/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;

import org.hibernate.id.IntegralDataTypeHolder;

/**
 * Performs optimization on an optimizable identifier generator.  Typically
 * this optimization takes the form of trying to ensure we do not have to
 * hit the database on each and every request to get an identifier value.
 * <p/>
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
	 * @param callback Callback to access the underlying value source.
	 * @return The generated identifier value.
	 */
	public Serializable generate(AccessCallback callback);

	/**
	 * A common means to access the last value obtained from the underlying
	 * source.  This is intended for testing purposes, since accessing the
	 * underlying database source directly is much more difficult.
	 *
	 * @return The last value we obtained from the underlying source;
	 * null indicates we have not yet consulted with the source.
	 */
	public IntegralDataTypeHolder getLastSourceValue();

	/**
	 * Retrieves the defined increment size.
	 *
	 * @return The increment size.
	 */
	public int getIncrementSize();

	/**
	 * Are increments to be applied to the values stored in the underlying
	 * value source?
	 *
	 * @return True if the values in the source are to be incremented
	 * according to the defined increment size; false otherwise, in which
	 * case the increment is totally an in memory construct.
	 */
	public boolean applyIncrementSizeToSourceValues();
}
