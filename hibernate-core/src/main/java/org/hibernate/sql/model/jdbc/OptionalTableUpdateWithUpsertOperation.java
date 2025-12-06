/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.internal.OptionalTableInsert;
import org.hibernate.sql.model.internal.OptionalTableUpdate;

import java.util.Arrays;
import java.util.Collections;

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
		if ( getTableDetails().getInsertDetails() != null
			&& getTableDetails().getInsertDetails().getCustomSql() != null
			|| !getValueBindings().isEmpty() ) {
			return super.createJdbcOptionalInsert( session );
		}
		else {
			// Ignore a primary key violation on insert when inserting just the primary key columns
			final TableMutation<? extends JdbcMutationOperation> tableInsert = new OptionalTableInsert(
					new MutatingTableReference( getTableDetails() ),
					getMutationTarget(),
					CollectionHelper.combine( getValueBindings(), getKeyBindings() ),
					Collections.emptyList(),
					getParameters(),
					null,
					Arrays.asList( ((EntityPersister) getMutationTarget()).getIdentifierColumnNames() )
			);

			final SessionFactoryImplementor factory = session.getSessionFactory();
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
