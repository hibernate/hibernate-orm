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
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandler;
import org.hibernate.query.sqm.mutation.spi.MultiTableHandlerBuildResult;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;


/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableInsertStrategy extends LocalTemporaryTableStrategy implements SqmMultiTableInsertStrategy {

	public LocalTemporaryTableInsertStrategy(EntityMappingType rootEntityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {
		this(
				rootEntityDescriptor,
				requireLocalTemporaryTableStrategy( runtimeModelCreationContext.getDialect() ),
				runtimeModelCreationContext
		);
	}

	private LocalTemporaryTableInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			TemporaryTableStrategy temporaryTableStrategy,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this(
				TemporaryTable.createEntityTable(
						runtimeModelCreationContext.getMetadata()
								.getEntityBinding( rootEntityDescriptor.getEntityName() ),
						basename -> temporaryTableStrategy.adjustTemporaryTableName( TemporaryTable.ENTITY_TABLE_PREFIX + basename ),
						TemporaryTableKind.LOCAL,
						runtimeModelCreationContext.getDialect(),
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	public LocalTemporaryTableInsertStrategy(
			TemporaryTable entityTable,
			SessionFactoryImplementor sessionFactory) {
		super( entityTable, sessionFactory );
	}

	@Override
	public MultiTableHandlerBuildResult buildHandler(SqmInsertStatement<?> sqmInsertStatement, DomainParameterXref domainParameterXref, DomainQueryExecutionContext context) {
		final MutableObject<JdbcParameterBindings> firstJdbcParameterBindings = new MutableObject<>();
		final MultiTableHandler multiTableHandler = new TableBasedInsertHandler(
				sqmInsertStatement,
				domainParameterXref,
				getTemporaryTable(),
				getTemporaryTableStrategy(),
				isDropIdTables(),
				session -> {
					throw new UnsupportedOperationException( "Unexpected call to access Session uid" );
				},
				context,
				firstJdbcParameterBindings
		);
		return new MultiTableHandlerBuildResult( multiTableHandler, firstJdbcParameterBindings.get() );
	}
}
