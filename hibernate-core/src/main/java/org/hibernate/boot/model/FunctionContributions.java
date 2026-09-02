/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.USE;

/// Boot-scoped access used by a [FunctionContributor] or [Dialect] to register
/// functions with the eventual [SqmFunctionRegistry].
///
/// Complete registrations during the active contribution callback. Do not
/// retain this object or its mutable registry after the callback returns.
/// Hibernate creates and supplies this callback; providers consume it but do
/// not implement or supply it.
///
/// @see FunctionContributor
/// @see Dialect#initializeFunctionRegistry(FunctionContributions)
/// @see org.hibernate.dialect.function.CommonFunctionFactory
///
/// @author Christian Beikov
@SPI(USE)
public interface FunctionContributions {

	/**
	 * The registry into which the contributions should be made.
	 */
	SqmFunctionRegistry getFunctionRegistry();

	/**
	 * Access to type information.
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Access to {@linkplain Service services}.
	 */
	ServiceRegistry getServiceRegistry();

	/**
	 * The {@linkplain Dialect SQL Dialect}.
	 */
	default Dialect getDialect() {
		return getTypeConfiguration().getCurrentBaseSqlTypeIndicators().getDialect();
	}
}
