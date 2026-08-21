/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
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
		super( mutationTarget, upsert, factory );
	}

	@Override
	protected JdbcMutationOperation createJdbcOptionalInsert(SharedSessionContractImplementor session) {
		final var tableDetails = getTableDetails();
		final var insertDetails = tableDetails.getInsertDetails();
		if ( insertDetails != null && insertDetails.getCustomSql() != null
				|| !getValueBindings().isEmpty() ) {
			return super.createJdbcOptionalInsert( session );
		}
		else {
			final var mutationTarget = (EntityPersister) getMutationTarget();
			// Ignore a primary key violation on insert when inserting just the primary key columns
			final var tableInsert = new OptionalTableInsert(
					new MutatingTableReference( tableDetails ),
					getMutationTarget(),
					combine( getValueBindings(), getKeyBindings() ),
					emptyList(),
					getParameters(),
					null,
					asList( mutationTarget.getIdentifierColumnNames() )
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
