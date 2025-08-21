/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * DeleteHandler for the in-line strategy
 *
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public class InlineDeleteHandler extends AbstractInlineHandler implements DeleteHandler {

	private final List<TableDeleter> tableDeleters;

	protected InlineDeleteHandler(
			MatchingIdRestrictionProducer matchingIdsPredicateProducer,
			SqmDeleteStatement<?> sqmStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		super( matchingIdsPredicateProducer, sqmStatement, domainParameterXref, context, firstJdbcParameterBindingsConsumer );
		final List<TableDeleter> tableDeleters = new ArrayList<>();

		final SoftDeleteMapping softDeleteMapping = getEntityDescriptor().getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			tableDeleters.add( createSoftDeleter() );
		}
		else {
			// delete from the tables
			final MutableInteger valueIndexCounter = new MutableInteger();
			SqmMutationStrategyHelper.visitCollectionTables(
					getEntityDescriptor(),
					pluralAttribute -> {
						// Skip deleting rows in collection tables if cascade delete is enabled
						if ( pluralAttribute.getSeparateCollectionTable() != null
							&& !pluralAttribute.getCollectionDescriptor().isCascadeDeleteEnabled() ) {
							// this collection has a separate collection table, meaning it is one of:
							//		1) element-collection
							//		2) many-to-many
							//		3) one-to many using a dedicated join-table
							//
							// in all of these cases, we should clean up the matching rows in the
							// collection table
							final ModelPart fkTargetPart = pluralAttribute.getKeyDescriptor().getTargetPart();
							final int valueIndex;
							if ( fkTargetPart.isEntityIdentifierMapping() ) {
								valueIndex = 0;
							}
							else {
								if ( valueIndexCounter.get() == 0 ) {
									valueIndexCounter.set(
											getEntityDescriptor().getIdentifierMapping().getJdbcTypeCount() );
								}
								valueIndex = valueIndexCounter.get();
								valueIndexCounter.plus( fkTargetPart.getJdbcTypeCount() );
							}
							final NamedTableReference targetTableReference = new NamedTableReference(
									pluralAttribute.getSeparateCollectionTable(),
									DeleteStatement.DEFAULT_ALIAS
							);

							tableDeleters.add( new TableDeleter(
									new DeleteStatement( targetTableReference, null ),
									() -> fkTargetPart::forEachSelectable,
									valueIndex,
									fkTargetPart
							) );
						}
					}
			);

			getEntityDescriptor().visitConstraintOrderedTables(
					(tableExpression, tableKeyColumnsVisitationSupplier) -> tableDeleters.add( new TableDeleter(
							new DeleteStatement(
									new NamedTableReference( tableExpression, DeleteStatement.DEFAULT_ALIAS ),
									null
							),
							tableKeyColumnsVisitationSupplier,
							0,
							null
					) )
			);
		}

		this.tableDeleters = tableDeleters;
	}

	private TableDeleter createSoftDeleter() {
		final EntityPersister entityDescriptor = getEntityDescriptor();
		final TableDetails softDeleteTable = entityDescriptor.getSoftDeleteTableDetails();
		final SoftDeleteMapping softDeleteMapping = entityDescriptor.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		final NamedTableReference targetTableReference = new NamedTableReference(
				softDeleteTable.getTableName(),
				DeleteStatement.DEFAULT_ALIAS
		);

		final Assignment softDeleteAssignment = softDeleteMapping.createSoftDeleteAssignment( targetTableReference );
		final UpdateStatement updateStatement = new UpdateStatement(
				targetTableReference,
				Collections.singletonList( softDeleteAssignment ),
				softDeleteMapping.createNonDeletedRestriction( targetTableReference )
		);

		return new TableDeleter( updateStatement, null, 0, entityDescriptor.getIdentifierMapping() );
	}

	@Override
	public int execute(JdbcParameterBindings jdbcParameterBindings, DomainQueryExecutionContext executionContext) {
		final List<Object> idsAndFks = MatchingIdSelectionHelper.selectMatchingIds(
				getMatchingIdsInterpretation(),
				jdbcParameterBindings,
				executionContext
		);

		if ( idsAndFks == null || idsAndFks.isEmpty() ) {
			return 0;
		}

		final List<Expression> inListExpressions = getMatchingIdsPredicateProducer().produceIdExpressionList( idsAndFks, getEntityDescriptor() );
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcMutationExecutor jdbcMutationExecutor = sessionFactory.getJdbcServices().getJdbcMutationExecutor();
		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );

		for ( TableDeleter tableDeleter : tableDeleters ) {
			jdbcMutationExecutor.execute(
					createMutation(
							tableDeleter,
							inListExpressions,
							executionContextAdapter
					),
					JdbcParameterBindings.NO_BINDINGS,
					sql -> session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql ),
					(integer, preparedStatement) -> {},
					executionContextAdapter
			);
		}

		return idsAndFks.size();
	}

	protected record TableDeleter(
			AbstractUpdateOrDeleteStatement statement,
			@Nullable Supplier<Consumer<SelectableConsumer>> tableKeyColumnsVisitationSupplier,
			int valueIndex,
			@Nullable ModelPart valueModelPart
	) {}

	// For Hibernate Reactive
	protected JdbcOperationQueryMutation createMutation(TableDeleter tableDeleter, List<Expression> inListExpressions, ExecutionContext executionContext) {
		final MutationStatement statement;
		if ( tableDeleter.statement instanceof UpdateStatement updateStatement ) {
			statement = new UpdateStatement(
					updateStatement,
					updateStatement.getTargetTable(),
					updateStatement.getFromClause(),
					updateStatement.getAssignments(),
					Predicate.combinePredicates(
							updateStatement.getRestriction(),
							getMatchingIdsPredicateProducer().produceRestriction(
									inListExpressions,
									getEntityDescriptor(),
									tableDeleter.valueIndex,
									tableDeleter.valueModelPart,
									updateStatement.getTargetTable(),
									tableDeleter.tableKeyColumnsVisitationSupplier,
									executionContext
							)
					),
					updateStatement.getReturningColumns()
			);
		}
		else {
			final DeleteStatement deleteStatement = (DeleteStatement) tableDeleter.statement;
			statement = new DeleteStatement(
					deleteStatement,
					deleteStatement.getTargetTable(),
					deleteStatement.getFromClause(),
					Predicate.combinePredicates(
							deleteStatement.getRestriction(),
							getMatchingIdsPredicateProducer().produceRestriction(
									inListExpressions,
									getEntityDescriptor(),
									tableDeleter.valueIndex,
									tableDeleter.valueModelPart,
									deleteStatement.getTargetTable(),
									tableDeleter.tableKeyColumnsVisitationSupplier,
									executionContext
							)
					),
					deleteStatement.getReturningColumns()
			);
		}
		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();
		return sqlAstTranslatorFactory.buildMutationTranslator( sessionFactory, statement )
				.translate( JdbcParameterBindings.NO_BINDINGS, executionContext.getQueryOptions() );
	}

	// For Hibernate Reactive
	protected List<TableDeleter> getTableDeleters() {
		return tableDeleters;
	}
}
