/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.HibernateException;

/**
 * Common support for {@link Optimizer} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractOptimizer implements Optimizer {
	protected final Class<?> returnClass;
	protected final int incrementSize;

	/**
	 * Construct an optimizer
	 *
	 * @param returnClass The expected id class.
	 * @param incrementSize The increment size
	 */
	AbstractOptimizer(Class<?> returnClass, int incrementSize) {
		if ( returnClass == null ) {
			throw new HibernateException( "return class is required" );
		}
		this.returnClass = returnClass;
		this.incrementSize = incrementSize;
	}

	/**
	 * Getter for property 'returnClass'.  This is the Java
	 * class which is used to represent the id (e.g. {@link Long}).
	 *
	 * @return Value for property 'returnClass'.
	 */
	public final Class<?> getReturnClass() {
		return returnClass;
	}

	@Override
	public final int getIncrementSize() {
		return incrementSize;
	}
}
