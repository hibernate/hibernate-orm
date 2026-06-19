/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.sql.spi.SqmTranslation;
import org.hibernate.query.sqm.sql.spi.StandardSqmTranslation;
import org.hibernate.query.sqm.tree.spi.SqmDmlStatement;
import org.hibernate.query.sqm.tree.spi.delete.SqmDeleteStatement;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.from.MutatingTableReferenceGroupWrapper;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildColumnReferenceExpression;

/**
 * @author Steve Ebersole
 */
public class SimpleDeleteQueryPlan extends SimpleNonSelectQueryPlan {

	private final EntityPersister entityDescriptor;

	private volatile List<JdbcOperationQueryMutation> collectionTableDeletes;

	public SimpleDeleteQueryPlan(
			EntityPersister entityDescriptor,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref) {
		super( sqmDelete, domainParameterXref );
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	protected Interpretation buildInterpretation(
			SqmDmlStatement<?> sqm,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		final var sqmInterpretation = super.buildInterpretation( sqm, domainParameterXref, context );

		final var factory = context.getSession().getFactory();
		final var statement =
				(AbstractUpdateOrDeleteStatement)
						sqmInterpretation.interpretation().statement();
		final ArrayList<JdbcOperationQueryMutation> collectionTableDeletes = new ArrayList<>();
		SqmMutationStrategyHelper.visitCollectionTableDeletes(
				entityDescriptor,
				(tableReference, attributeMapping) -> {
					final TableGroup collectionTableGroup = new MutatingTableReferenceGroupWrapper(
							new NavigablePath( attributeMapping.getRootPathName() ),
							attributeMapping,
							(NamedTableReference) tableReference
					);

					final MutableObject<Predicate> additionalPredicate = new MutableObject<>();
					attributeMapping.applyBaseRestrictions(
							p -> additionalPredicate.set( Predicate.combinePredicates( additionalPredicate.get(), p ) ),
							collectionTableGroup,
							getDialect( factory ).getDmlTargetColumnQualifierSupport()
									== DmlTargetColumnQualifierSupport.TABLE_ALIAS,
							context.getSession().getLoadQueryInfluencers().getEnabledFilters(),
							false,
							null,
							null
					);

					if ( statement.getRestriction() == null ) {
						return additionalPredicate.get();
					}

					final var fkDescriptor = attributeMapping.getKeyDescriptor();
					final var fkColumnExpression = buildColumnReferenceExpression(
							collectionTableGroup,
							fkDescriptor.getKeyPart(),
							null,
							factory
					);

					final var matchingIdSubQuery = new QuerySpec( false );

					final var tableGroup = new MutatingTableReferenceGroupWrapper(
							new NavigablePath( attributeMapping.getRootPathName() ),
							attributeMapping,
							statement.getTargetTable()
					);
					final var fkTargetColumnExpression = buildColumnReferenceExpression(
							tableGroup,
							fkDescriptor.getTargetPart(),
							null,
							factory
					);
					matchingIdSubQuery.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, fkTargetColumnExpression ) );
					matchingIdSubQuery.getFromClause().addRoot( tableGroup );
					matchingIdSubQuery.applyPredicate( statement.getRestriction() );

					return Predicate.combinePredicates(
							additionalPredicate.get(),
							new InSubQueryPredicate( fkColumnExpression, matchingIdSubQuery, false )
					);
				},
				sqmInterpretation.jdbcParameterBindings(),
				context.getQueryOptions(),
				collectionTableDeletes::add
		);
		this.collectionTableDeletes = collectionTableDeletes;
		return sqmInterpretation;
	}

	private static Dialect getDialect(SessionFactoryImplementor factory) {
		return factory.getJdbcServices().getDialect();
	}

	// For Hibernate Reactive
	protected List<JdbcOperationQueryMutation> getCollectionTableDeletes() {
		return collectionTableDeletes;
	}

	@Override
	protected int execute(CacheableSqmInterpretation<MutationStatement, JdbcOperationQueryMutation> sqmInterpretation, JdbcParameterBindings jdbcParameterBindings, ExecutionContext executionContext) {
		final var factory = executionContext.getSession().getFactory();
		final var jdbcMutationExecutor = factory.getJdbcServices().getJdbcMutationExecutor();
		for ( var delete : collectionTableDeletes ) {
			jdbcMutationExecutor.execute(
					delete,
					jdbcParameterBindings,
					sql -> executionContext.getSession()
							.getJdbcCoordinator()
							.getStatementPreparer()
							.prepareStatement( sql ),
					(integer, preparedStatement) -> {},
					executionContext
			);
		}
		return super.execute( sqmInterpretation, jdbcParameterBindings, executionContext );
	}

	@Override
	protected SqmTranslation<? extends MutationStatement> buildTranslation(SqmDmlStatement<?> sqm, DomainParameterXref domainParameterXref, DomainQueryExecutionContext executionContext) {
		final var sqmTranslation = super.buildTranslation( sqm, domainParameterXref, executionContext );
		final var columnMapping = entityDescriptor.getSoftDeleteMapping();
		if ( columnMapping == null ) {
			return sqmTranslation;
		}
		else {
			final var sqlDeleteAst = (AbstractUpdateOrDeleteStatement) sqmTranslation.getSqlAst();
			final var targetTable = sqlDeleteAst.getTargetTable();
			final var assignment = columnMapping.createSoftDeleteAssignment( targetTable );
			return new StandardSqmTranslation<>(
					new UpdateStatement(
							sqlDeleteAst,
							targetTable,
							sqlDeleteAst.getMutationTarget(),
							sqlDeleteAst.getFromClause(),
							singletonList( assignment ),
							sqlDeleteAst.getRestriction(),
							sqlDeleteAst.getReturningColumns()
					),
					sqmTranslation.getJdbcParamsBySqmParam(),
					sqmTranslation.getSqmParameterMappingModelTypeResolutions(),
					sqmTranslation.getSqlExpressionResolver(),
					sqmTranslation.getFromClauseAccess()
			);
		}
	}

}
