/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.dialect.temptable.TemporaryTableSessionUidColumn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.mutation.internal.InsertHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.SqmInsertStrategyHelper;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;

import org.jboss.logging.Logger;

/**
* @author Christian Beikov
*/
public class TableBasedInsertHandler implements InsertHandler {
	private static final Logger log = Logger.getLogger( TableBasedInsertHandler.class );

	public interface ExecutionDelegate {
		int execute(ExecutionContext executionContext);
	}

	private final SqmInsertStatement<?> sqmInsertStatement;
	private final SessionFactoryImplementor sessionFactory;

	private final TemporaryTable entityTable;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;
	private final JdbcParameter sessionUidParameter;

	public TableBasedInsertHandler(
			SqmInsertStatement<?> sqmInsert,
			DomainParameterXref domainParameterXref,
			TemporaryTable entityTable,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SessionFactoryImplementor sessionFactory) {
		this.sqmInsertStatement = sqmInsert;
		this.afterUseAction = afterUseAction;
		this.sessionFactory = sessionFactory;
		this.entityTable = entityTable;
		this.sessionUidAccess = sessionUidAccess;
		this.domainParameterXref = domainParameterXref;

		final TemporaryTableSessionUidColumn sessionUidColumn = entityTable.getSessionUidColumn();
		if ( sessionUidColumn == null ) {
			this.sessionUidParameter = null;
		}
		else {
			this.sessionUidParameter = new JdbcParameterImpl( sessionUidColumn.getJdbcMapping() );
		}
	}

	public SqmInsertStatement<?> getSqmInsertStatement() {
		return sqmInsertStatement;
	}

	@Override
	public int execute(DomainQueryExecutionContext executionContext) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Starting multi-table insert execution - %s",
					getSqmInsertStatement().getTarget().getModel().getName()
			);
		}

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );
		return resolveDelegate( executionContext ).execute( executionContextAdapter );
	}

	protected ExecutionDelegate resolveDelegate(DomainQueryExecutionContext executionContext) {
		final EntityPersister entityDescriptor = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( getSqmInsertStatement().getTarget().getEntityName() );

		final MultiTableSqmMutationConverter converterDelegate = new MultiTableSqmMutationConverter(
				entityDescriptor,
				getSqmInsertStatement(),
				getSqmInsertStatement().getTarget(),
				domainParameterXref,
				executionContext.getQueryOptions(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getQueryParameterBindings(),
				sessionFactory
		);

		final TableGroup insertingTableGroup = converterDelegate.getMutatingTableGroup();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the insertion target using our special converter, collecting
		// information about the target paths

		final List<Assignment> targetPathColumns = new ArrayList<>();
		final NamedTableReference entityTableReference = new NamedTableReference(
				entityTable.getTableExpression(),
				TemporaryTable.DEFAULT_ALIAS,
				true
		);
		final InsertSelectStatement insertStatement = new InsertSelectStatement( entityTableReference );

		final BaseSqmToSqlAstConverter.AdditionalInsertValues additionalInsertValues = converterDelegate.visitInsertionTargetPaths(
				(assigable, columnReferences) -> {
					insertStatement.addTargetColumnReferences( columnReferences );
					targetPathColumns.add( new Assignment( assigable, (Expression) assigable ) );
				},
				sqmInsertStatement,
				entityDescriptor,
				insertingTableGroup
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the where-clause using our special converter, collecting information
		// about the restrictions

		final TemporaryTableSessionUidColumn sessionUidColumn = entityTable.getSessionUidColumn();
		if ( sqmInsertStatement instanceof SqmInsertSelectStatement ) {
			final QueryPart queryPart = converterDelegate.visitQueryPart( ( (SqmInsertSelectStatement<?>) sqmInsertStatement ).getSelectQueryPart() );
			queryPart.visitQuerySpecs(
					querySpec -> {
						if ( additionalInsertValues.applySelections( querySpec, sessionFactory ) ) {
							final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
									.get( entityTable.getColumns().size() - ( sessionUidColumn == null ? 1 : 2 ) );
							final ColumnReference columnReference = new ColumnReference(
									(String) null,
									rowNumberColumn.getColumnName(),
									false,
									null,
									rowNumberColumn.getJdbcMapping()
							);
							insertStatement.getTargetColumns().set(
									insertStatement.getTargetColumns().size() - 1,
									columnReference
							);
							targetPathColumns.set(
									targetPathColumns.size() - 1,
									new Assignment( columnReference, columnReference )
							);
						}
						else if ( entityDescriptor.getGenerator() instanceof OptimizableGenerator ) {
							final Optimizer optimizer = ( (OptimizableGenerator) entityDescriptor.getGenerator() ).getOptimizer();
							if ( optimizer != null && optimizer.getIncrementSize() > 1 ) {
								if ( !sessionFactory.getJdbcServices().getDialect().supportsWindowFunctions() ) {
									return;
								}
								final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
										.get( entityTable.getColumns().size() - ( sessionUidColumn == null ? 1 : 2 ) );
								final ColumnReference columnReference = new ColumnReference(
										(String) null,
										rowNumberColumn.getColumnName(),
										false,
										null,
										rowNumberColumn.getJdbcMapping()
								);
								insertStatement.getTargetColumns().add( columnReference );
								targetPathColumns.add( new Assignment( columnReference, columnReference ) );
								querySpec.getSelectClause().addSqlSelection(
										new SqlSelectionImpl(
												0,
												SqmInsertStrategyHelper.createRowNumberingExpression(
														querySpec,
														sessionFactory
												)
										)
								);
							}
						}
						if ( sessionUidColumn != null ) {
							final ColumnReference sessionUidColumnReference = new ColumnReference(
									(String) null,
									sessionUidColumn.getColumnName(),
									false,
									null,
									sessionUidColumn.getJdbcMapping()
							);
							querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl(
									insertStatement.getTargetColumns().size(),
									sessionUidParameter
							) );
							insertStatement.getTargetColumns().add( sessionUidColumnReference );
							targetPathColumns.add( new Assignment( sessionUidColumnReference, sessionUidParameter ) );
						}
					}
			);
			insertStatement.setSourceSelectStatement( queryPart );
		}
		else {
			// Add the row number column if there is one
			final Generator generator = entityDescriptor.getGenerator();
			final BasicType<?> rowNumberType;
			if ( generator instanceof OptimizableGenerator ) {
				final Optimizer optimizer = ( (OptimizableGenerator) generator ).getOptimizer();
				if ( optimizer != null && optimizer.getIncrementSize() > 1 ) {
					final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
							.get( entityTable.getColumns().size() - ( sessionUidColumn == null ? 1 : 2 ) );
					rowNumberType = (BasicType<?>) rowNumberColumn.getJdbcMapping();
					final ColumnReference columnReference = new ColumnReference(
							(String) null,
							rowNumberColumn.getColumnName(),
							false,
							null,
							rowNumberColumn.getJdbcMapping()
					);
					insertStatement.getTargetColumns().add( columnReference );
					targetPathColumns.add( new Assignment( columnReference, columnReference ) );
				}
				else {
					rowNumberType = null;
				}
			}
			else {
				rowNumberType = null;
			}
			if ( sessionUidColumn != null ) {
				final ColumnReference sessionUidColumnReference = new ColumnReference(
						(String) null,
						sessionUidColumn.getColumnName(),
						false,
						null,
						sessionUidColumn.getJdbcMapping()
				);
				insertStatement.getTargetColumns().add( sessionUidColumnReference );
				targetPathColumns.add( new Assignment( sessionUidColumnReference, sessionUidParameter ) );
			}
			final List<SqmValues> sqmValuesList = ( (SqmInsertValuesStatement<?>) sqmInsertStatement ).getValuesList();
			final List<Values> valuesList = new ArrayList<>( sqmValuesList.size() );
			for ( int i = 0; i < sqmValuesList.size(); i++ ) {
				final Values values = converterDelegate.visitValues( sqmValuesList.get( i ) );
				additionalInsertValues.applyValues( values );
				if ( rowNumberType != null ) {
					values.getExpressions().add(
							new QueryLiteral<>(
									rowNumberType.getJavaTypeDescriptor()
											.wrap( i + 1, sessionFactory.getWrapperOptions() ),
									rowNumberType
							)
					);
				}
				if ( sessionUidParameter != null ) {
					values.getExpressions().add( sessionUidParameter );
				}
				valuesList.add( values );
			}
			insertStatement.setValuesList( valuesList );
		}
		final ConflictClause conflictClause = converterDelegate.visitConflictClause( sqmInsertStatement.getConflictClause() );
		converterDelegate.pruneTableGroupJoins();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, but the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( insertingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( insertingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < insertingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( insertingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		return buildExecutionDelegate(
				sqmInsertStatement,
				converterDelegate,
				entityTable,
				afterUseAction,
				sessionUidAccess,
				domainParameterXref,
				insertingTableGroup,
				tableReferenceByAlias,
				targetPathColumns,
				insertStatement,
				conflictClause,
				sessionUidParameter,
				executionContext
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected ExecutionDelegate buildExecutionDelegate(
			SqmInsertStatement<?> sqmInsert,
			MultiTableSqmMutationConverter sqmConverter,
			TemporaryTable entityTable,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			TableGroup insertingTableGroup,
			Map<String, TableReference> tableReferenceByAlias,
			List<Assignment> assignments,
			InsertSelectStatement insertStatement,
			ConflictClause conflictClause,
			JdbcParameter sessionUidParameter,
			DomainQueryExecutionContext executionContext) {
		return new InsertExecutionDelegate(
				sqmInsertStatement,
				sqmConverter,
				entityTable,
				afterUseAction,
				sessionUidAccess,
				domainParameterXref,
				insertingTableGroup,
				tableReferenceByAlias,
				assignments,
				insertStatement,
				conflictClause,
				sessionUidParameter,
				executionContext
		);
	}


	private void collectTableReference(
			TableReference tableReference,
			BiConsumer<String, TableReference> consumer) {
		consumer.accept( tableReference.getIdentificationVariable(), tableReference );
	}

	private void collectTableReference(
			TableReferenceJoin tableReferenceJoin,
			BiConsumer<String, TableReference> consumer) {
		collectTableReference( tableReferenceJoin.getJoinedTableReference(), consumer );
	}


}
