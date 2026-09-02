/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.SPI;
import org.hibernate.service.JavaServiceLoadable;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Contributes custom HQL functions to the eventual
/// [org.hibernate.query.sqm.function.SqmFunctionRegistry].
///
/// A provider normally exposes its implementation through the Java
/// [java.util.ServiceLoader] facility. An application may instead supply one
/// through
/// [org.hibernate.cfg.Configuration#registerFunctionContributor(FunctionContributor)]
/// or [org.hibernate.boot.MetadataBuilder#applyFunctions(FunctionContributor)].
///
/// Complete all registration during [#contributeFunctions]. Do not retain the
/// supplied [FunctionContributions] or its mutable registry beyond the
/// callback. Use a contributor for application- or library-wide functions; a
/// Dialect should instead override
/// [org.hibernate.dialect.Dialect#initializeFunctionRegistry(FunctionContributions)].
///
/// Contributors are invoked in ascending [#ordinal()] order. Higher ordinals
/// therefore replace registrations made under the same key by lower ordinals.
///
/// @see org.hibernate.query.sqm.function.SqmFunctionRegistry
/// @see org.hibernate.cfg.Configuration#registerFunctionContributor(FunctionContributor)
/// @see org.hibernate.boot.MetadataBuilder#applyFunctions(FunctionContributor)
///
/// @author Karel Maesen
@JavaServiceLoadable
@SPI({ USE, IMPLEMENT, SUPPLY })
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
