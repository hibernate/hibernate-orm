/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.StaleObjectStateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.internal.lock.LockingHelper;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.spi.NoRowException;
import org.hibernate.sql.results.spi.SingleResultConsumer;

import java.util.List;
import java.util.Locale;

/**
 * LockingStrategy implementation which uses Hibernate's SQL AST
 * mechanism for applying pessimistic locks.
 *
 * @author Steve Ebersole
 */
public class SqlAstBasedLockingStrategy implements LockingStrategy {
	private final EntityMappingType entityToLock;
	private final LockMode lockMode;
	private final Locking.Scope lockScope;

	public SqlAstBasedLockingStrategy(EntityPersister lockable, LockMode lockMode, Locking.Scope lockScope) {
		this.entityToLock = lockable.getRootEntityDescriptor();
		this.lockMode = lockMode;
		this.lockScope = lockScope;
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			int timeout,
			SharedSessionContractImplementor session)
				throws StaleObjectStateException, LockingStrategyException {
		final var factory = session.getFactory();

		final var lockOptions = new LockOptions( lockMode );
		lockOptions.setScope( lockScope );
		lockOptions.setTimeOut( timeout );

		final var rootQuerySpec = new QuerySpec( true );
		final var entityPath = new NavigablePath( entityToLock.getRootPathName() );
		final var idMapping = entityToLock.getIdentifierMapping();

		// NOTE: there are 2 possible ways to handle the select list for the query...
		// 		1) use normal `idMapping.createDomainResult`.  for simple ids, this is fine; however,
		//			for composite ids, this would require a proper implementation of `FetchProcessor`
		//			(the parts of the composition are considered `Fetch`es). `FetchProcessor` is not
		//			a trivial thing to implement though.  this would be the "best" approach though.
		//			look at simplifying LoaderSelectBuilder.visitFetches for reusability
		//		2) for now, we'll just manually build the selection list using "one of" the id columns
		//			and manually build a simple `BasicResult`
		final var sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				// todo (db-locking) : look to simplify LoaderSelectBuilder.visitFetches for reusability
				(fetchParent, creationState) -> ImmutableFetchList.EMPTY,

				true,
				new LoadQueryInfluencers( factory ),
				factory.getSqlTranslationEngine()
		);

		final var rootTableGroup = entityToLock.createRootTableGroup(
				true,
				entityPath,
				null,
				null,
				() -> p -> {
				},
				sqlAstCreationState
		);
		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( entityPath, rootTableGroup );

		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var firstIdColumn = idMapping.getSelectable( 0 );
		sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression( rootTableGroup.getPrimaryTableReference(), firstIdColumn ),
				firstIdColumn.getJdbcMapping().getJdbcJavaType(),
				null,
				session.getTypeConfiguration()
		);
		final BasicResult<Object> idResult =
				new BasicResult<>( 0, null, idMapping.getJdbcMapping( 0 ) );

		final var versionMapping = entityToLock.getVersionMapping();

		final int jdbcTypeCount = idMapping.getJdbcTypeCount();
		final int jdbcParamCount = versionMapping == null ? jdbcTypeCount : jdbcTypeCount + 1;
		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParamCount );
		idMapping.breakDownJdbcValues(
				id,
				(valueIndex, value, jdbcValueMapping) -> handleRestriction(
						value,
						jdbcValueMapping,
						rootQuerySpec,
						sqlAstCreationState,
						rootTableGroup,
						jdbcParameterBindings
				),
				session
		);

		if ( versionMapping != null ) {
			versionMapping.breakDownJdbcValues(
					version,
					(valueIndex, value, jdbcValueMapping) -> handleRestriction(
							value,
							jdbcValueMapping,
							rootQuerySpec,
							sqlAstCreationState,
							rootTableGroup,
							jdbcParameterBindings
					),
					session
			);
		}

		final var selectStatement = new SelectStatement( rootQuerySpec, List.of( idResult ) );
		final JdbcSelect selectOperation =
				session.getDialect().getSqlAstTranslatorFactory()
						.buildSelectTranslator( factory, selectStatement )
						.translate( jdbcParameterBindings, sqlAstCreationState );

		final var lockingExecutionContext = new LockingExecutionContext( session );
		try {
			factory.getJdbcServices().getJdbcSelectExecutor()
					.executeQuery(
							selectOperation,
							jdbcParameterBindings,
							lockingExecutionContext,
							null,
							idResult.getResultJavaType().getJavaTypeClass(),
							1,
							SingleResultConsumer.instance()
					);

			if ( lockOptions.getScope() == Locking.Scope.INCLUDE_COLLECTIONS ) {
				SqmMutationStrategyHelper.visitCollectionTables( entityToLock, (attribute) -> {
					final var collectionToLock = (PersistentCollection<?>) attribute.getValue( object );
					LockingHelper.lockCollectionTable(
							attribute,
							lockMode,
							lockOptions.getTimeout(),
							collectionToLock,
							lockingExecutionContext
					);
				} );
			}
		}
		catch (LockTimeoutException lockTimeout) {
			throw new PessimisticEntityLockException(
					object,
					String.format( Locale.ROOT, "Lock timeout exceeded attempting to lock row(s) for %s", object ),
					lockTimeout
			);
		}
		catch (NoRowException noRow) {
			if ( !entityToLock.optimisticLockStyle().isNone() ) {
				final String entityName = entityToLock.getEntityName();
				final var statistics = session.getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( entityName );
				}
				throw new StaleObjectStateException( entityName, id,
						"No rows were returned from JDBC query for versioned entity" );
			}
			else {
				throw noRow;
			}
		}
	}

	private static void handleRestriction(
			Object value,
			SelectableMapping jdbcValueMapping,
			QuerySpec rootQuerySpec,
			LoaderSqlAstCreationState sqlAstCreationState,
			TableGroup rootTableGroup,
			JdbcParameterBindings jdbcParameterBindings) {
		final var jdbcParameter = new SqlTypedMappingJdbcParameter( jdbcValueMapping );
		rootQuerySpec.applyPredicate(
				new ComparisonPredicate(
						sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
								rootTableGroup.getTableReference( jdbcValueMapping.getContainingTableExpression() ),
								jdbcValueMapping
						),
						ComparisonOperator.EQUAL,
						jdbcParameter
				)
		);

		final var jdbcMapping = jdbcValueMapping.getJdbcMapping();
		jdbcParameterBindings.addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, value ) );
	}

}
