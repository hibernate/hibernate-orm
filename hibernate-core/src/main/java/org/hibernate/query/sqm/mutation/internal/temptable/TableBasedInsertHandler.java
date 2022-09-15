/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.mutation.internal.InsertHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.update.Assignment;
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

	private final EntityPersister entityDescriptor;

	TableBasedInsertHandler(
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

		final String targetEntityName = sqmInsert.getTarget().getEntityName();
		this.entityDescriptor = sessionFactory.getRuntimeMetamodels().getMappingMetamodel().getEntityDescriptor( targetEntityName );
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

	private ExecutionDelegate resolveDelegate(DomainQueryExecutionContext executionContext) {
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

		final Map<SqmParameter<?>, List<List<JdbcParameter>>> parameterResolutions;
		if ( domainParameterXref.getSqmParameterCount() == 0 ) {
			parameterResolutions = Collections.emptyMap();
		}
		else {
			parameterResolutions = new IdentityHashMap<>();
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the insertion target using our special converter, collecting
		// information about the target paths

		final List<Assignment> targetPathColumns = new ArrayList<>();
		final Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions = new LinkedHashMap<>();
		final NamedTableReference entityTableReference = new NamedTableReference(
				entityTable.getTableExpression(),
				TemporaryTable.DEFAULT_ALIAS,
				true,
				sessionFactory
		);
		final InsertStatement insertStatement = new InsertStatement( entityTableReference );

		final BaseSqmToSqlAstConverter.AdditionalInsertValues additionalInsertValues = converterDelegate.visitInsertionTargetPaths(
				(assigable, columnReferences) -> {
					insertStatement.addTargetColumnReferences( columnReferences );
					targetPathColumns.add( new Assignment( assigable, (Expression) assigable ) );
				},
				sqmInsertStatement,
				entityDescriptor,
				insertingTableGroup,
				(sqmParameter, mappingType, jdbcParameters) -> {
					parameterResolutions.computeIfAbsent(
							sqmParameter,
							k -> new ArrayList<>( 1 )
					).add( jdbcParameters );
					paramTypeResolutions.put( sqmParameter, mappingType );
				}
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the where-clause using our special converter, collecting information
		// about the restrictions

		if ( sqmInsertStatement instanceof SqmInsertSelectStatement ) {
			final QueryPart queryPart = converterDelegate.visitQueryPart( ( (SqmInsertSelectStatement<?>) sqmInsertStatement ).getSelectQueryPart() );
			queryPart.visitQuerySpecs(
					querySpec -> {
						if ( additionalInsertValues.applySelections( querySpec, sessionFactory ) ) {
							final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
									.get( entityTable.getColumns().size() - 1 );
							final ColumnReference columnReference = new ColumnReference(
									(String) null,
									rowNumberColumn.getColumnName(),
									false,
									null,
									null,
									rowNumberColumn.getJdbcMapping(),
									sessionFactory
							);
							insertStatement.getTargetColumnReferences().set(
									insertStatement.getTargetColumnReferences().size() - 1,
									columnReference
							);
							targetPathColumns.set(
									targetPathColumns.size() - 1,
									new Assignment( columnReference, columnReference )
							);
						}
						else if ( entityDescriptor.getIdentifierGenerator() instanceof OptimizableGenerator ) {
							final Optimizer optimizer = ( (OptimizableGenerator) entityDescriptor.getIdentifierGenerator() ).getOptimizer();
							if ( optimizer != null && optimizer.getIncrementSize() > 1 ) {
								if ( !sessionFactory.getJdbcServices().getDialect().supportsWindowFunctions() ) {
									return;
								}
								final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
										.get( entityTable.getColumns().size() - 1 );
								final ColumnReference columnReference = new ColumnReference(
										(String) null,
										rowNumberColumn.getColumnName(),
										false,
										null,
										null,
										rowNumberColumn.getJdbcMapping(),
										sessionFactory
								);
								insertStatement.getTargetColumnReferences().add( columnReference );
								targetPathColumns.add( new Assignment( columnReference, columnReference ) );
								final BasicType<Integer> rowNumberType = sessionFactory.getTypeConfiguration()
										.getBasicTypeForJavaType( Integer.class );
								querySpec.getSelectClause().addSqlSelection(
										new SqlSelectionImpl(
												1,
												0,
												new Over<>(
														new SelfRenderingFunctionSqlAstExpression(
																StandardFunctions.ROW_NUMBER,
																(appender, args, walker) -> appender.appendSql(
																		"row_number()" ),
																Collections.emptyList(),
																rowNumberType,
																rowNumberType
														),
														Collections.emptyList(),
														Collections.emptyList()
												)
										)
								);
							}
						}
					}
			);
			insertStatement.setSourceSelectStatement( queryPart );
		}
		else {
			// Add the row number column if there is one
			final IdentifierGenerator generator = entityDescriptor.getIdentifierGenerator();
			final BasicType<?> rowNumberType;
			if ( generator instanceof OptimizableGenerator ) {
				final Optimizer optimizer = ( (OptimizableGenerator) generator ).getOptimizer();
				if ( optimizer != null && optimizer.getIncrementSize() > 1 ) {
					final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
							.get( entityTable.getColumns().size() - 1 );
					rowNumberType = (BasicType<?>) rowNumberColumn.getJdbcMapping();
					final ColumnReference columnReference = new ColumnReference(
							(String) null,
							rowNumberColumn.getColumnName(),
							false,
							null,
							null,
							rowNumberColumn.getJdbcMapping(),
							sessionFactory
					);
					insertStatement.getTargetColumnReferences().add( columnReference );
					targetPathColumns.add( new Assignment( columnReference, columnReference ) );
				}
				else {
					rowNumberType = null;
				}
			}
			else {
				rowNumberType = null;
			}
			final List<SqmValues> sqmValuesList = ( (SqmInsertValuesStatement<?>) sqmInsertStatement ).getValuesList();
			final List<Values> valuesList = new ArrayList<>( sqmValuesList.size() );
			for ( int i = 0; i < sqmValuesList.size(); i++ ) {
				final Values values = converterDelegate.visitValues( sqmValuesList.get( i ) );
				additionalInsertValues.applyValues( values );
				if ( rowNumberType != null ) {
					values.getExpressions().add(
							new QueryLiteral<>(
									i + 1,
									rowNumberType
							)
					);
				}
				valuesList.add( values );
			}
			insertStatement.setValuesList( valuesList );
		}
		converterDelegate.pruneTableGroupJoins();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, but the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( insertingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( insertingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < insertingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( insertingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		return new InsertExecutionDelegate(
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
				parameterResolutions,
				paramTypeResolutions,
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
