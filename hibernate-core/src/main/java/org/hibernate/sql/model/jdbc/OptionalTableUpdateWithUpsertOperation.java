/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.internal.OptionalTableInsert;
import org.hibernate.sql.model.internal.OptionalTableUpdate;



import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.collections.CollectionHelper.combine;

/**
 * Uses {@link org.hibernate.sql.model.internal.OptionalTableInsert} for the insert operation,
 * to avoid primary key constraint violations when inserting only primary key columns.
 */
public class OptionalTableUpdateWithUpsertOperation extends OptionalTableUpdateOperation {

	public OptionalTableUpdateWithUpsertOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate upsert,
			@SuppressWarnings("unused") SessionFactoryImplementor factory) {
		super( mutationTarget, upsert, determineRowCountExpectation( upsert ), factory );
	}

	private static Expectation determineRowCountExpectation(OptionalTableUpdate upsert) {
		if ( !hasUpdatableColumnBindings( upsert ) ) {
			// If the table has no value bindings for updatable columns, i.e. is an id-only table,
			// we have to relax the row count expectation
			final boolean isOptional = upsert.getMutatingTable().getTableMapping().isOptional();
			final ModelPartContainer targetPart = upsert.getMutationTarget().getTargetPart();
			if ( targetPart instanceof EntityMappingType entityMappingType
				&& (isOptional || entityMappingType.getVersionMapping() == null) ) {
				return new Expectation.OptionalRowCount();
			}
		}
		return upsert.getExpectation();
	}

	private static boolean hasUpdatableColumnBindings(OptionalTableUpdate upsert) {
		if ( !upsert.getValueBindings().isEmpty() ) {
			for ( ColumnValueBinding valueBinding : upsert.getValueBindings() ) {
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
			final var mutationTarget = (EntityPersister) getMutationTarget();
			// Ignore a primary key violation on insert
			final int tableIndex = ArrayHelper.indexOf( mutationTarget.getTableNames(), tableDetails.getTableName() );
			final var tableInsert = new OptionalTableInsert(
					new MutatingTableReference( tableDetails ),
					getMutationTarget(),
					combine( getValueBindings(), getKeyBindings() ),
					emptyList(),
					getParameters(),
					null,
					asList( mutationTarget.getKeyColumns( tableIndex ) )
			);

			final var factory = session.getSessionFactory();
			return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
					.buildModelMutationTranslator( tableInsert, factory )
					.translate( null, MutationQueryOptions.INSTANCE );
		}
	}

	@Override
	public String toString() {
		return "OptionalTableUpdateWithUpsertOperation(" + getTableDetails() + ")";
	}
}
