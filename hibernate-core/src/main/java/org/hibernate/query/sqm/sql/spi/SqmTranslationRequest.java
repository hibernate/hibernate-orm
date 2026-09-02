/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.spi;

import java.util.Objects;

import org.hibernate.SPI;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.tree.spi.SqmDmlStatement;
import org.hibernate.query.sqm.tree.spi.SqmStatement;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationContext;

import static org.hibernate.SPI.Role.USE;

/// The complete supported input for creating one SQM-to-SQL-AST translator.
///
/// A [SqmTranslatorFactory] consumes the matching request subtype and returns a
/// fresh translator for it. Factories and translators must not retain a request
/// after that translation.
///
/// @param <S> the SQM statement type
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public sealed interface SqmTranslationRequest<S extends SqmStatement<?>>
		permits SqmTranslationRequest.Select, SqmTranslationRequest.Mutation {
	/// The SQM statement to translate.
	S statement();

	/// Query options effective for this translation.
	QueryOptions queryOptions();

	/// The parameter correlations belonging to the SQM statement.
	SqmParameterMapping parameterMapping();

	/// The current domain query parameter bindings.
	QueryParameterBindings parameterBindings();

	/// Load and fetch-plan influences effective for this translation.
	LoadQueryInfluencers loadQueryInfluencers();

	/// Mapping-model and SessionFactory services used to create the SQL AST.
	SqlAstCreationContext creationContext();

	/// Whether duplicate select items may be removed during translation.
	default boolean deduplicateSelectionItems() {
		return false;
	}

	/// Request to translate an SQM select statement.
	record Select(
			SqmSelectStatement<?> statement,
			QueryOptions queryOptions,
			SqmParameterMapping parameterMapping,
			QueryParameterBindings parameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext,
			boolean deduplicateSelectionItems)
			implements SqmTranslationRequest<SqmSelectStatement<?>> {
		public Select {
			Objects.requireNonNull( statement, "statement" );
			Objects.requireNonNull( queryOptions, "queryOptions" );
			Objects.requireNonNull( parameterMapping, "parameterMapping" );
			Objects.requireNonNull( parameterBindings, "parameterBindings" );
			Objects.requireNonNull( loadQueryInfluencers, "loadQueryInfluencers" );
			Objects.requireNonNull( creationContext, "creationContext" );
		}
	}

	/// Request to translate an SQM mutation statement.
	record Mutation(
			SqmDmlStatement<?> statement,
			QueryOptions queryOptions,
			SqmParameterMapping parameterMapping,
			QueryParameterBindings parameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationContext creationContext)
			implements SqmTranslationRequest<SqmDmlStatement<?>> {
		public Mutation {
			Objects.requireNonNull( statement, "statement" );
			Objects.requireNonNull( queryOptions, "queryOptions" );
			Objects.requireNonNull( parameterMapping, "parameterMapping" );
			Objects.requireNonNull( parameterBindings, "parameterBindings" );
			Objects.requireNonNull( loadQueryInfluencers, "loadQueryInfluencers" );
			Objects.requireNonNull( creationContext, "creationContext" );
		}
	}
}
