/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi.mutation.jdbc;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.OptionalTableInsert;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.collections.CollectionHelper.combine;

/**
 * Uses {@link org.hibernate.sql.ast.spi.model.OptionalTableInsert} for the insert operation,
 * to avoid primary key constraint violations when inserting only primary key columns.
 */
public final class OptionalTableUpdateWithUpsertOperation extends OptionalTableUpdateOperation {

	public OptionalTableUpdateWithUpsertOperation(
			OptionalTableUpdate upsert,
			boolean versionedTarget) {
		super( upsert.getMutationTarget(), upsert, determineRowCountExpectation( upsert, versionedTarget ) );
	}

	private static Expectation determineRowCountExpectation(OptionalTableUpdate upsert, boolean versionedTarget) {
		if ( !hasUpdatableColumnBindings( upsert ) ) {
			// If the table has no value bindings for updatable columns, i.e. is an id-only table,
			// we have to relax the row count expectation
			final boolean isOptional = upsert.getMutatingTable().getTableMapping().isOptional();
			if ( isOptional || !versionedTarget ) {
				return new Expectation.OptionalRowCount();
			}
		}
		return upsert.getExpectation();
	}

	private static boolean hasUpdatableColumnBindings(OptionalTableUpdate upsert) {
		if ( !upsert.getValueBindings().isEmpty() ) {
			for ( var valueBinding : upsert.getValueBindings() ) {
				if ( valueBinding.isAttributeUpdatable() ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected JdbcMutationOperation createJdbcOptionalInsert(SharedSessionContractImplementor session) {
		final var tableDetails = getTableDetails();
		final var insertDetails = tableDetails.getInsertDetails();
		if ( insertDetails != null && insertDetails.getCustomSql() != null ) {
			return super.createJdbcOptionalInsert( session );
		}
		else {
			// Ignore a primary key violation on insert
			final var tableInsert = new OptionalTableInsert(
					new MutatingTableReference( tableDetails ),
					getMutationTarget(),
					combine( getValueBindings(), getKeyBindings() ),
					emptyList(),
					getParameters(),
					null,
					getKeyBindings().stream()
							.map( binding -> binding.getColumnReference().getColumnExpression() )
							.toList()
			);

			final var factory = session.getSessionFactory();
			return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
					.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( factory, tableInsert ) )
					.translate( null, MutationQueryOptions.INSTANCE );
		}
	}

	@Override
	public String toString() {
		return "OptionalTableUpdateWithUpsertOperation(" + getTableDetails() + ")";
	}
}
