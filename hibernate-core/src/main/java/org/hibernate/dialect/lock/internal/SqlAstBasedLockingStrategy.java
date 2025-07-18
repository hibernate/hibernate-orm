/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.spi.NoRowException;
import org.hibernate.sql.results.spi.SingleResultConsumer;
import org.hibernate.stat.spi.StatisticsImplementor;

import java.util.List;
import java.util.Locale;

/**
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
			SharedSessionContractImplementor session) throws StaleObjectStateException, LockingStrategyException {
		final SessionFactoryImplementor factory = session.getFactory();

		final LockOptions lockOptions = new LockOptions( lockMode );
		lockOptions.setScope( lockScope );
		lockOptions.setTimeOut( timeout );

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final NavigablePath entityPath = new NavigablePath( entityToLock.getRootPathName() );
		final EntityIdentifierMapping idMapping = entityToLock.getIdentifierMapping();

		// NOTE: there are 2 possible ways to handle the select list for the query...
		// 		1) use normal `idMapping.createDomainResult`.  for simple ids, this is fine; however,
		//			for composite ids, this would require a proper implementation of `FetchProcessor`
		//			(the parts of the composition are considered `Fetch`es). `FetchProcessor` is not
		//			a trivial thing to implement though.  this would be the "best" approach though.
		//			look at simplifying LoaderSelectBuilder.visitFetches for reusability
		//		2) for now, we'll just manually build the selection list using "one of" the id columns
		//			and manually build a simple `BasicResult`
		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				(fetchParent, creationState) -> {
					// todo (db-locking) : look to simplify LoaderSelectBuilder.visitFetches for reusability
					return ImmutableFetchList.EMPTY;
				},
				true,
				new LoadQueryInfluencers( factory ),
				factory.getSqlTranslationEngine()
		);

		final TableGroup rootTableGroup = entityToLock.createRootTableGroup(
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

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final SelectableMapping firstIdColumn = idMapping.getSelectable( 0 );
		sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression( rootTableGroup.getPrimaryTableReference(), firstIdColumn ),
				firstIdColumn.getJdbcMapping().getJdbcJavaType(),
				null,
				session.getTypeConfiguration()
		);
		final BasicResult<Object> idResult = new BasicResult<>( 0, null, idMapping.getJdbcMapping( 0 ) );

		final int jdbcParamCount = entityToLock.getVersionMapping() != null
				? idMapping.getJdbcTypeCount() + 1
				: idMapping.getJdbcTypeCount();
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParamCount );
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

		if ( entityToLock.getVersionMapping() != null ) {
			entityToLock.getVersionMapping().breakDownJdbcValues(
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

		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec, List.of( idResult ) );
		final JdbcOperationQuerySelect selectOperation = session
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( factory, selectStatement )
				.translate( jdbcParameterBindings, sqlAstCreationState );

		final JdbcSelectExecutor jdbcSelectExecutor = factory.getJdbcServices().getJdbcSelectExecutor();
		final LockingExecutionContext lockingExecutionContext = new LockingExecutionContext( session );

		try {
			jdbcSelectExecutor.executeQuery(
					selectOperation,
					jdbcParameterBindings,
					lockingExecutionContext,
					null,
					idResult.getResultJavaType().getJavaTypeClass(),
					1,
					SingleResultConsumer.instance()
			);
		}
		catch (LockTimeoutException e) {
			throw new PessimisticEntityLockException(
					object,
					String.format( Locale.ROOT, "Lock timeout exceeded attempting to lock row(s) for %s", object ),
					e
			);
		}
		catch (NoRowException e) {
			if ( entityToLock.optimisticLockStyle() != OptimisticLockStyle.NONE ) {
				final StatisticsImplementor statistics = session.getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( entityToLock.getEntityName() );
				}
				throw new StaleObjectStateException(
						entityToLock.getEntityName(),
						id,
						"No rows were returned from JDBC query for versioned entity"
				);
			}
			else {
				throw e;
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
		final JdbcMapping jdbcMapping = jdbcValueMapping.getJdbcMapping();
		final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
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

		final JdbcParameterBindingImpl jdbcParameterBinding = new JdbcParameterBindingImpl( jdbcMapping, value );
		jdbcParameterBindings.addBinding( jdbcParameter, jdbcParameterBinding );
	}

}
