/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.SemanticException;
import org.hibernate.query.results.TableGroupImpl;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 *
 * @author Christian Beikov
 */
public class CteUpdateHandler extends AbstractCteMutationHandler implements UpdateHandler {

	private static final String UPDATE_RESULT_TABLE_NAME_PREFIX = "update_cte_";
	private static final String INSERT_RESULT_TABLE_NAME_PREFIX = "insert_cte_";

	public CteUpdateHandler(
			CteTable cteTable,
			SqmUpdateStatement<?> sqmStatement,
			DomainParameterXref domainParameterXref,
			CteMutationStrategy strategy,
			SessionFactoryImplementor sessionFactory) {
		super( cteTable, sqmStatement, domainParameterXref, strategy, sessionFactory );
	}

	@Override
	protected void addDmlCtes(
			CteContainer statement,
			CteStatement idSelectCte,
			MultiTableSqmMutationConverter sqmConverter,
			Map<SqmParameter<?>, List<JdbcParameter>> parameterResolutions,
			SessionFactoryImplementor factory) {
		final TableGroup updatingTableGroup = sqmConverter.getMutatingTableGroup();
		final SqmUpdateStatement<?> updateStatement = (SqmUpdateStatement<?>) getSqmDeleteOrUpdateStatement();
		final EntityMappingType entityDescriptor = getEntityDescriptor();

		final AbstractEntityPersister entityPersister = (AbstractEntityPersister) entityDescriptor.getEntityPersister();
		final String rootEntityName = entityPersister.getRootEntityName();
		final EntityPersister rootEntityDescriptor = factory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( rootEntityName );

		final String hierarchyRootTableName = ( (Joinable) rootEntityDescriptor ).getTableName();
		final TableReference hierarchyRootTableReference = updatingTableGroup.resolveTableReference(
				updatingTableGroup.getNavigablePath(),
				hierarchyRootTableName
		);
		assert hierarchyRootTableReference != null;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the set-clause using our special converter, collecting
		// information about the assignments
		final SqmSetClause setClause = updateStatement.getSetClause();
		final List<Assignment> assignments = sqmConverter.visitSetClause( setClause );
		for ( Map.Entry<SqmParameter<?>, List<List<JdbcParameter>>> entry : sqmConverter.getJdbcParamsBySqmParam().entrySet() ) {
			parameterResolutions.put( entry.getKey(), entry.getValue().get( entry.getValue().size() - 1 ) );
		}
		sqmConverter.addVersionedAssignment( assignments::add, updateStatement );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, but the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( updatingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < updatingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( updatingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		final Map<TableReference, List<Assignment>> assignmentsByTable = CollectionHelper.mapOfSize(
				updatingTableGroup.getTableReferenceJoins().size() + 1
		);

		for ( int i = 0; i < assignments.size(); i++ ) {
			final Assignment assignment = assignments.get( i );
			final List<ColumnReference> assignmentColumnRefs = assignment.getAssignable().getColumnReferences();

			TableReference assignmentTableReference = null;

			for ( int c = 0; c < assignmentColumnRefs.size(); c++ ) {
				final ColumnReference columnReference = assignmentColumnRefs.get( c );
				final TableReference tableReference = resolveTableReference(
						columnReference,
						tableReferenceByAlias
				);

				// TODO: this could be fixed by introducing joins to DML statements
				if ( assignmentTableReference != null && !assignmentTableReference.equals( tableReference ) ) {
					throw new IllegalStateException( "Assignment referred to columns from multiple tables" );
				}

				assignmentTableReference = tableReference;
			}
			assert assignmentTableReference != null;

			List<Assignment> assignmentsForTable = assignmentsByTable.get( assignmentTableReference );
			if ( assignmentsForTable == null ) {
				assignmentsForTable = new ArrayList<>();
				assignmentsByTable.put( assignmentTableReference, assignmentsForTable );
			}
			assignmentsForTable.add( assignment );
		}

		// For nullable tables we have to also generate an insert CTE
		for (int i = 0; i < entityPersister.getTableSpan(); i++) {
			if ( entityPersister.isNullableTable( i ) ) {
				final String tableExpression = entityPersister.getTableName( i );
				final TableReference updatingTableReference = updatingTableGroup.getTableReference(
						updatingTableGroup.getNavigablePath(),
						tableExpression,
						true
				);
				final List<Assignment> assignmentsForInsert = assignmentsByTable.get( updatingTableReference );
				if ( assignmentsForInsert == null ) {
					continue;
				}
				final String insertCteTableName = getInsertCteTableName( tableExpression );
				if ( statement.getCteStatement( insertCteTableName ) != null ) {
					// Since secondary tables could appear multiple times, we have to skip duplicates
					continue;
				}
				final CteTable dmlResultCte = new CteTable(
						insertCteTableName,
						idSelectCte.getCteTable().getCteColumns()
				);
				final NamedTableReference dmlTableReference = resolveUnionTableReference(
						updatingTableReference,
						tableExpression
				);
				final NamedTableReference existsTableReference = new NamedTableReference(
						tableExpression,
						"dml_"
				);
				final List<ColumnReference> existsKeyColumns = new ArrayList<>( idSelectCte.getCteTable().getCteColumns().size() );
				final String[] keyColumns = entityPersister.getKeyColumns( i );
				entityPersister.getIdentifierMapping().forEachSelectable(
						(selectionIndex, selectableMapping) -> {
							existsKeyColumns.add(
									new ColumnReference(
											existsTableReference,
											keyColumns[selectionIndex],
											selectableMapping.getJdbcMapping()
									)
							);
						}
				);

				// Copy the subquery contents into a root query
				final QuerySpec querySpec = createIdSubQuery( idSelectCte, null, factory ).asRootQuery();

				// Prepare a not exists sub-query to avoid violating constraints
				final QuerySpec existsQuerySpec = new QuerySpec( false );
				existsQuerySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								new QueryLiteral<>(
										1,
										factory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class )
								)
						)
				);
				existsQuerySpec.getFromClause().addRoot(
						new TableGroupImpl(
								null,
								null,
								existsTableReference,
								entityPersister
						)
				);

				existsQuerySpec.applyPredicate(
						new ComparisonPredicate(
								asExpression( existsKeyColumns ),
								ComparisonOperator.EQUAL,
								asExpression( querySpec.getSelectClause() )
						)
				);

				querySpec.applyPredicate(
						new ExistsPredicate(
								existsQuerySpec,
								true,
								factory.getTypeConfiguration().getBasicTypeForJavaType( Boolean.class )
						)
				);

				// Collect the target column references from the key expressions
				final List<ColumnReference> targetColumnReferences = new ArrayList<>( existsKeyColumns );
				// And transform assignments to target column references and selections
				for ( Assignment assignment : assignmentsForInsert ) {
					targetColumnReferences.addAll( assignment.getAssignable().getColumnReferences() );
					querySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									assignment.getAssignedValue()
							)
					);
				}

				final InsertSelectStatement dmlStatement = new InsertSelectStatement( dmlTableReference, existsKeyColumns );
				dmlStatement.addTargetColumnReferences( targetColumnReferences.toArray( new ColumnReference[0] ) );
				dmlStatement.setSourceSelectStatement( querySpec );
				statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
			}
		}

		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableColumnsVisitationSupplier) -> {
					final String cteTableName = getCteTableName( tableExpression );
					if ( statement.getCteStatement( cteTableName ) != null ) {
						// Since secondary tables could appear multiple times, we have to skip duplicates
						return;
					}
					final CteTable dmlResultCte = new CteTable(
							cteTableName,
							idSelectCte.getCteTable().getCteColumns()
					);
					final TableReference updatingTableReference = updatingTableGroup.getTableReference(
							updatingTableGroup.getNavigablePath(),
							tableExpression,
							true
					);
					final List<Assignment> assignmentList = assignmentsByTable.get( updatingTableReference );
					if ( assignmentList == null ) {
						return;
					}
					final NamedTableReference dmlTableReference = resolveUnionTableReference(
							updatingTableReference,
							tableExpression
					);
					final List<ColumnReference> columnReferences = new ArrayList<>( idSelectCte.getCteTable().getCteColumns().size() );
					tableColumnsVisitationSupplier.get().accept(
							(index, selectable) -> columnReferences.add(
									new ColumnReference(
											dmlTableReference,
											selectable
									)
							)
					);
					final MutationStatement dmlStatement = new UpdateStatement(
							dmlTableReference,
							assignmentList,
							createIdSubQueryPredicate( columnReferences, idSelectCte, factory ),
							columnReferences
					);
					statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
				}
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

	private TableReference resolveTableReference(
			ColumnReference columnReference,
			Map<String, TableReference> tableReferenceByAlias) {
		final TableReference tableReferenceByQualifier = tableReferenceByAlias.get( columnReference.getQualifier() );
		if ( tableReferenceByQualifier != null ) {
			return tableReferenceByQualifier;
		}

		throw new SemanticException( "Assignment referred to column of a joined association: " + columnReference );
	}

	@Override
	protected String getCteTableName(String tableExpression) {
		final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
		if ( Identifier.isQuoted( tableExpression ) ) {
			tableExpression = QualifiedNameParser.INSTANCE.parse( tableExpression ).getObjectName().getText();
		}
		return Identifier.toIdentifier( UPDATE_RESULT_TABLE_NAME_PREFIX + tableExpression ).render( dialect );
	}

	protected String getInsertCteTableName(String tableExpression) {
		final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
		if ( Identifier.isQuoted( tableExpression ) ) {
			tableExpression = tableExpression.substring( 1, tableExpression.length() - 1 );
		}
		return Identifier.toIdentifier( INSERT_RESULT_TABLE_NAME_PREFIX + tableExpression ).render( dialect );
	}

	private Expression asExpression(SelectClause selectClause) {
		final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
		if ( sqlSelections.size() == 1 ) {
			return sqlSelections.get( 0 ).getExpression();
		}
		final List<Expression> expressions = new ArrayList<>( sqlSelections.size() );
		for ( SqlSelection sqlSelection : sqlSelections ) {
			expressions.add( sqlSelection.getExpression() );
		}
		return new SqlTuple( expressions, null );
	}

	private Expression asExpression(List<ColumnReference> columnReferences) {
		if ( columnReferences.size() == 1 ) {
			return columnReferences.get( 0 );
		}
		return new SqlTuple( columnReferences, null );
	}
}
