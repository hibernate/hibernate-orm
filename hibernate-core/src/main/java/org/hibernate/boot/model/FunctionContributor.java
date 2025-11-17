/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.service.JavaServiceLoadable;

/**
 * An object that contributes custom HQL functions, eventually to a
 * {@link org.hibernate.query.sqm.function.SqmFunctionRegistry}, via an
 * instance of {@link FunctionContributions}.
 * <ul>
 * <li>
 *     The most common way to integrate a {@code FunctionContributor} is by
 *     making it discoverable via the Java {@link java.util.ServiceLoader}
 *     facility.
 * <li>
 *     Alternatively, a {@code FunctionContributor} may be programmatically supplied to
 *     {@link org.hibernate.cfg.Configuration#registerFunctionContributor(FunctionContributor)}
 *     or even {@link org.hibernate.boot.MetadataBuilder#applyFunctions(FunctionContributor)}.
 * </ul>
 *
 * @see org.hibernate.query.sqm.function.SqmFunctionRegistry
 *
 * @author Karel Maesen
 */
@JavaServiceLoadable
public interface FunctionContributor {

	/**
	 * Contribute functions
	 *
	 * @param functionContributions The target for the contributions
	 */
	void contributeFunctions(FunctionContributions functionContributions);

	/**
	 * Determines order in which the contributions will be applied
	 * (lowest ordinal first).
	 * <p>
	 * The range 0-500 is reserved for Hibernate, range 500-1000 for libraries and
	 * 1000-Integer.MAX_VALUE for user-defined FunctionContributors.
	 * <p>
	 * Contributions from higher precedence contributors (higher numbers) effectively override
	 * contributions from lower precedence.  E.g. if a contributor with precedence 1000 contributes a
	 * function named {@code "max"}, that will override Hibernate's standard function of that name.
	 *
	 * @return the ordinal for this FunctionContributor
	 */
	default int ordinal(){
		return 1000;
	}
}
