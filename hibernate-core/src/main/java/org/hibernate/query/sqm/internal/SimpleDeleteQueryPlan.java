/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.StandardSqmTranslation;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.MutatingTableReferenceGroupWrapper;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		final Interpretation sqmInterpretation =
				super.buildInterpretation( sqm, domainParameterXref, context );

		final SessionFactoryImplementor factory = context.getSession().getFactory();
		final AbstractUpdateOrDeleteStatement statement =
				(AbstractUpdateOrDeleteStatement) sqmInterpretation.interpretation().statement();
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
							factory.getJdbcServices().getDialect().getDmlTargetColumnQualifierSupport() == DmlTargetColumnQualifierSupport.TABLE_ALIAS,
							context.getSession().getLoadQueryInfluencers().getEnabledFilters(),
							false,
							null,
							null
					);

					if ( statement.getRestriction() == null ) {
						return additionalPredicate.get();
					}

					final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();
					final Expression fkColumnExpression = MappingModelCreationHelper.buildColumnReferenceExpression(
							collectionTableGroup,
							fkDescriptor.getKeyPart(),
							null,
							factory
					);

					final QuerySpec matchingIdSubQuery = new QuerySpec( false );

					final MutatingTableReferenceGroupWrapper tableGroup = new MutatingTableReferenceGroupWrapper(
							new NavigablePath( attributeMapping.getRootPathName() ),
							attributeMapping,
							statement.getTargetTable()
					);
					final Expression fkTargetColumnExpression = MappingModelCreationHelper.buildColumnReferenceExpression(
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

	// For Hibernate Reactive
	protected List<JdbcOperationQueryMutation> getCollectionTableDeletes() {
		return collectionTableDeletes;
	}

	@Override
	protected int execute(CacheableSqmInterpretation<MutationStatement, JdbcOperationQueryMutation> sqmInterpretation, JdbcParameterBindings jdbcParameterBindings, ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		final JdbcMutationExecutor jdbcMutationExecutor = factory.getJdbcServices().getJdbcMutationExecutor();
		for ( JdbcOperationQueryMutation delete : collectionTableDeletes ) {
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
		final SqmTranslation<? extends MutationStatement> sqmTranslation =
				super.buildTranslation( sqm, domainParameterXref, executionContext );

		final SoftDeleteMapping columnMapping = entityDescriptor.getSoftDeleteMapping();
		if ( columnMapping == null ) {
			return sqmTranslation;
		}
		else {
			final AbstractUpdateOrDeleteStatement sqlDeleteAst =
					(AbstractUpdateOrDeleteStatement) sqmTranslation.getSqlAst();
			final NamedTableReference targetTable = sqlDeleteAst.getTargetTable();
			final Assignment assignment = columnMapping.createSoftDeleteAssignment( targetTable );

			return new StandardSqmTranslation<>(
					new UpdateStatement(
							sqlDeleteAst,
							targetTable,
							sqlDeleteAst.getFromClause(),
							Collections.singletonList( assignment ),
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
