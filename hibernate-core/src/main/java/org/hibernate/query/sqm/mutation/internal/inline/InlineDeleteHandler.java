/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.StatementCreatorHelper;

import static org.hibernate.boot.model.internal.SoftDeleteHelper.createNonSoftDeletedRestriction;
import static org.hibernate.boot.model.internal.SoftDeleteHelper.createSoftDeleteAssignment;

/**
 * DeleteHandler for the in-line strategy
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public class InlineDeleteHandler implements DeleteHandler {
	private final MatchingIdRestrictionProducer matchingIdsPredicateProducer;
	private final SqmDeleteStatement<?> sqmDeleteStatement;
	private final DomainParameterXref domainParameterXref;

	private final DomainQueryExecutionContext executionContext;

	private final SessionFactoryImplementor sessionFactory;
	private final SqlAstTranslatorFactory sqlAstTranslatorFactory;
	private final JdbcMutationExecutor jdbcMutationExecutor;

	protected InlineDeleteHandler(
			MatchingIdRestrictionProducer matchingIdsPredicateProducer,
			SqmDeleteStatement<?> sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		this.sqmDeleteStatement = sqmDeleteStatement;

		this.domainParameterXref = domainParameterXref;
		this.matchingIdsPredicateProducer = matchingIdsPredicateProducer;

		this.executionContext = context;

		this.sessionFactory = executionContext.getSession().getFactory();
		this.sqlAstTranslatorFactory = sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();
		this.jdbcMutationExecutor = sessionFactory.getJdbcServices().getJdbcMutationExecutor();
	}

	@Override
	public int execute(DomainQueryExecutionContext executionContext) {
		final List<Object> idsAndFks = MatchingIdSelectionHelper.selectMatchingIds(
				sqmDeleteStatement,
				domainParameterXref,
				executionContext
		);

		if ( idsAndFks == null || idsAndFks.isEmpty() ) {
			return 0;
		}

		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final String mutatingEntityName = sqmDeleteStatement.getTarget().getModel().getHibernateEntityName();
		final EntityMappingType entityDescriptor = factory.getRuntimeMetamodels().getEntityMappingType( mutatingEntityName );

		final List<Expression> inListExpressions = matchingIdsPredicateProducer.produceIdExpressionList( idsAndFks, entityDescriptor );
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( domainParameterXref.getQueryParameterCount() );

		// delete from the tables
		final MutableInteger valueIndexCounter = new MutableInteger();
		SqmMutationStrategyHelper.visitCollectionTables(
				entityDescriptor,
				pluralAttribute -> {
					if ( pluralAttribute.getSeparateCollectionTable() != null ) {
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
								valueIndexCounter.set( entityDescriptor.getIdentifierMapping().getJdbcTypeCount() );
							}
							valueIndex = valueIndexCounter.get();
							valueIndexCounter.plus( fkTargetPart.getJdbcTypeCount() );
						}

						executeDelete(
								pluralAttribute.getSeparateCollectionTable(),
								entityDescriptor,
								() -> fkTargetPart::forEachSelectable,
								inListExpressions,
								valueIndex,
								fkTargetPart,
								jdbcParameterBindings,
								executionContext
						);
					}
				}
		);

		final SoftDeleteMapping softDeleteMapping = entityDescriptor.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			performSoftDelete(
					entityDescriptor,
					inListExpressions,
					jdbcParameterBindings,
					executionContext
			);
		}
		else {
			entityDescriptor.visitConstraintOrderedTables(
					(tableExpression, tableKeyColumnsVisitationSupplier) -> executeDelete(
							tableExpression,
							entityDescriptor,
							tableKeyColumnsVisitationSupplier,
							inListExpressions,
							0,
							null,
							jdbcParameterBindings,
							executionContext
					)
			);
		}

		return idsAndFks.size();
	}

	/**
	 * Perform a soft-delete, which just needs to update the root table
	 */
	private void performSoftDelete(
			EntityMappingType entityDescriptor,
			List<Expression> idExpressions,
			JdbcParameterBindings jdbcParameterBindings,
			DomainQueryExecutionContext executionContext) {
		final TableDetails softDeleteTable = entityDescriptor.getSoftDeleteTableDetails();
		final SoftDeleteMapping softDeleteMapping = entityDescriptor.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		final NamedTableReference targetTableReference = new NamedTableReference(
				softDeleteTable.getTableName(),
				DeleteStatement.DEFAULT_ALIAS
		);

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );

		final Predicate matchingIdsPredicate = matchingIdsPredicateProducer.produceRestriction(
				idExpressions,
				entityDescriptor,
				0,
				entityDescriptor.getIdentifierMapping(),
				targetTableReference,
				null,
				executionContextAdapter
		);

		final Predicate predicate = Predicate.combinePredicates(
				matchingIdsPredicate,
				createNonSoftDeletedRestriction( targetTableReference, softDeleteMapping )
		);

		final Assignment softDeleteAssignment = createSoftDeleteAssignment(
				targetTableReference,
				softDeleteMapping
		);

		final UpdateStatement updateStatement = new UpdateStatement(
				targetTableReference,
				Collections.singletonList( softDeleteAssignment ),
				predicate
		);

		final JdbcOperationQueryMutation jdbcOperation = sqlAstTranslatorFactory
				.buildMutationTranslator( sessionFactory, updateStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		jdbcMutationExecutor.execute(
				jdbcOperation,
				jdbcParameterBindings,
				this::prepareQueryStatement,
				(integer, preparedStatement) -> {},
				executionContextAdapter
		);
	}

	private void executeDelete(
			String targetTableExpression,
			EntityMappingType entityDescriptor,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnsVisitationSupplier,
			List<Expression> idExpressions,
			int valueIndex,
			ModelPart valueModelPart,
			JdbcParameterBindings jdbcParameterBindings,
			DomainQueryExecutionContext executionContext) {
		final NamedTableReference targetTableReference = new NamedTableReference(
				targetTableExpression,
				DeleteStatement.DEFAULT_ALIAS
		);

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );

		final Predicate matchingIdsPredicate = matchingIdsPredicateProducer.produceRestriction(
				idExpressions,
				entityDescriptor,
				valueIndex,
				valueModelPart,
				targetTableReference,
				tableKeyColumnsVisitationSupplier,
				executionContextAdapter
		);

		final DeleteStatement deleteStatement = new DeleteStatement( targetTableReference, matchingIdsPredicate );

		final JdbcOperationQueryMutation jdbcOperation = sqlAstTranslatorFactory.buildMutationTranslator( sessionFactory, deleteStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		jdbcMutationExecutor.execute(
				jdbcOperation,
				jdbcParameterBindings,
				this::prepareQueryStatement,
				(integer, preparedStatement) -> {},
				executionContextAdapter
		);
	}

	private PreparedStatement prepareQueryStatement(String sql) {
		return StatementCreatorHelper.prepareQueryStatement( sql, executionContext.getSession() );
	}
}
