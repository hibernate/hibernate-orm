/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.spi.SqmStatement;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;

import static org.hibernate.SPI.Role.USE;

/// The parameter-correlation operations available during one SQM-to-SQL-AST
/// translation.
///
/// A translator may resolve the domain query parameter represented by an SQM
/// parameter and register SQM expansions produced for a multi-valued binding.
/// It must not retain or mutate the mapping outside the current translation.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public interface SqmParameterMapping {
	/// The parameter resolutions belonging to the translated SQM statement.
	SqmStatement.ParameterResolutions getParameterResolutions();

	/// Resolve the domain query parameter represented by the SQM parameter.
	@Nullable
	QueryParameterImplementor<?> getQueryParameter(SqmParameter<?> sqmParameter);

	/// Register an SQM parameter produced by expanding a multi-valued binding.
	void addExpansion(
			QueryParameterImplementor<?> queryParameter,
			SqmParameter<?> original,
			SqmParameter<?> expansion);
}
