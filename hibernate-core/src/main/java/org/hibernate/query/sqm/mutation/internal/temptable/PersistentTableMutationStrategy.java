/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandler;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;



/**
 * This is a strategy that mimics temporary tables for databases which do not support
 * temporary tables.  It follows a pattern similar to the ANSI SQL definition of global
 * temporary table using a "session id" column to segment rows from the various sessions.
 *
 * @author Steve Ebersole
 */
public class PersistentTableMutationStrategy extends PersistentTableStrategy implements SqmMultiTableMutationStrategy {

	public PersistentTableMutationStrategy(EntityMappingType rootEntityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {
		this(
				rootEntityDescriptor,
				runtimeModelCreationContext.getDialect().getPersistentTemporaryTableStrategy(),
				runtimeModelCreationContext
		);
	}

	private PersistentTableMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			TemporaryTableStrategy temporaryTableStrategy,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this(
				TemporaryTable.createIdTable(
						runtimeModelCreationContext.getBootModel()
								.getEntityBinding( rootEntityDescriptor.getEntityName() ),
						basename -> temporaryTableStrategy.adjustTemporaryTableName( TemporaryTable.ID_TABLE_PREFIX + basename ),
						TemporaryTableKind.PERSISTENT,
						runtimeModelCreationContext.getDialect(),
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	public PersistentTableMutationStrategy(
			TemporaryTable idTable,
			SessionFactoryImplementor sessionFactory) {
		super( idTable, sessionFactory );
	}

	@Override
	public MultiTableHandlerBuildResult buildHandler(SqmDeleteOrUpdateStatement<?> sqmStatement, DomainParameterXref domainParameterXref, DomainQueryExecutionContext context) {
		final MutableObject<JdbcParameterBindings> firstJdbcParameterBindings = new MutableObject<>();
		final MultiTableHandler multiTableHandler = sqmStatement instanceof SqmDeleteStatement<?> sqmDelete
				? buildHandler( sqmDelete, domainParameterXref, context, firstJdbcParameterBindings )
				: buildHandler( (SqmUpdateStatement<?>) sqmStatement, domainParameterXref, context, firstJdbcParameterBindings );
		return new MultiTableHandlerBuildResult( multiTableHandler, firstJdbcParameterBindings.get() );
	}

	public MultiTableHandler buildHandler(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		return new TableBasedUpdateHandler(
				sqmUpdate,
				domainParameterXref,
				getTemporaryTable(),
				getTemporaryTableStrategy(),
				false,
				session -> session.getSessionIdentifier().toString(),
				context,
				firstJdbcParameterBindingsConsumer
		);
	}

	public MultiTableHandler buildHandler(
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		final EntityPersister rootDescriptor = context.getSession().getFactory().getMappingMetamodel()
				.getEntityDescriptor( sqmDelete.getRoot().getEntityName() );
		if ( rootDescriptor.getSoftDeleteMapping() != null ) {
			return new TableBasedSoftDeleteHandler(
					sqmDelete,
					domainParameterXref,
					getTemporaryTable(),
					getTemporaryTableStrategy(),
					false,
					session -> session.getSessionIdentifier().toString(),
					context,
					firstJdbcParameterBindingsConsumer
			);
		}
		else {
			return new TableBasedDeleteHandler(
					sqmDelete,
					domainParameterXref,
					getTemporaryTable(),
					getTemporaryTableStrategy(),
					false,
					session -> session.getSessionIdentifier().toString(),
					context,
					firstJdbcParameterBindingsConsumer
			);
		}
	}
}
