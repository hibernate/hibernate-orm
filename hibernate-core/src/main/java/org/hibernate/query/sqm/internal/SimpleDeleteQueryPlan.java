/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.from.MutatingTableReferenceGroupWrapper;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * @author Steve Ebersole
 */
public class SimpleDeleteQueryPlan implements NonSelectQueryPlan {
	private final EntityMappingType entityDescriptor;
	private final SqmDeleteStatement<?> sqmDelete;
	private final DomainParameterXref domainParameterXref;

	private JdbcOperationQueryMutation jdbcOperation;

	private SqmTranslation<? extends AbstractUpdateOrDeleteStatement> sqmInterpretation;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;

	public SimpleDeleteQueryPlan(
			EntityMappingType entityDescriptor,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref) {
		assert entityDescriptor.getEntityName().equals( sqmDelete.getTarget().getEntityName() );

		this.entityDescriptor = entityDescriptor;
		this.sqmDelete = sqmDelete;
		this.domainParameterXref = domainParameterXref;
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		BulkOperationCleanupAction.schedule( executionContext.getSession(), sqmDelete );

		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();
		SqlAstTranslator<? extends JdbcOperationQueryMutation> sqlAstTranslator = null;
		if ( jdbcOperation == null ) {
			sqlAstTranslator = createTranslator( executionContext );
		}

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				factory.getRuntimeMetamodels().getMappingMetamodel(),
				sqmInterpretation.getFromClauseAccess()::findTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmInterpretation.getSqmParameterMappingModelTypeResolutions().get(parameter);
					}
				},
				session
		);

		if ( jdbcOperation != null
				&& ! jdbcOperation.isCompatibleWith( jdbcParameterBindings, executionContext.getQueryOptions() ) ) {
			sqlAstTranslator = createTranslator( executionContext );
		}

		if ( sqlAstTranslator != null ) {
			jdbcOperation = sqlAstTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
		}

		final boolean missingRestriction = sqmInterpretation.getSqlAst().getRestriction() == null;
		if ( missingRestriction ) {
			assert domainParameterXref.getSqmParameterCount() == 0;
			assert jdbcParamsXref.isEmpty();
		}

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.usingLockingAndPaging( executionContext );

		SqmMutationStrategyHelper.cleanUpCollectionTables(
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
							executionContext.getSession().getLoadQueryInfluencers().getEnabledFilters(),
							false,
							null,
							null
					);

					if ( missingRestriction ) {
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
							sqmInterpretation.getSqlAst().getTargetTable()
					);
					final Expression fkTargetColumnExpression = MappingModelCreationHelper.buildColumnReferenceExpression(
							tableGroup,
							fkDescriptor.getTargetPart(),
							sqmInterpretation.getSqlExpressionResolver(),
							factory
					);
					matchingIdSubQuery.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, fkTargetColumnExpression ) );

					matchingIdSubQuery.getFromClause().addRoot(
							tableGroup
					);

					matchingIdSubQuery.applyPredicate( sqmInterpretation.getSqlAst().getRestriction() );

					return Predicate.combinePredicates(
							additionalPredicate.get(),
							new InSubQueryPredicate( fkColumnExpression, matchingIdSubQuery, false )
					);
				},
				( missingRestriction ? JdbcParameterBindings.NO_BINDINGS : jdbcParameterBindings ),
				executionContextAdapter
		);

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcOperation,
				jdbcParameterBindings,
				sql -> session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContextAdapter
		);
	}

	protected SqlAstTranslator<? extends JdbcOperationQueryMutation> createTranslator(DomainQueryExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		final SqmTranslator<? extends MutationStatement> translator = factory.getQueryEngine().getSqmTranslatorFactory().createMutationTranslator(
				sqmDelete,
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getQueryParameterBindings(),
				executionContext.getSession().getLoadQueryInfluencers(),
				factory
		);
		//noinspection unchecked
		sqmInterpretation = (SqmTranslation<? extends AbstractUpdateOrDeleteStatement>) translator.translate();

		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
				domainParameterXref,
				sqmInterpretation::getJdbcParamsBySqmParam
		);
		final MutationStatement ast;
		if ( entityDescriptor.getSoftDeleteMapping() == null ) {
			ast = sqmInterpretation.getSqlAst();
		}
		else {
			final AbstractUpdateOrDeleteStatement sqlDeleteAst = sqmInterpretation.getSqlAst();
			final NamedTableReference targetTable = sqlDeleteAst.getTargetTable();
			final SoftDeleteMapping columnMapping = getEntityDescriptor().getSoftDeleteMapping();
			final ColumnReference columnReference = new ColumnReference( targetTable, columnMapping );
			//noinspection rawtypes,unchecked
			final JdbcLiteral jdbcLiteral = new JdbcLiteral( columnMapping.getDeletedLiteralValue(), columnMapping.getJdbcMapping() );
			final Assignment assignment = new Assignment( columnReference, jdbcLiteral );

			ast = new UpdateStatement(
					targetTable,
					Collections.singletonList( assignment ),
					sqlDeleteAst.getRestriction()
			);
		}
		return factory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, ast );
	}

}
