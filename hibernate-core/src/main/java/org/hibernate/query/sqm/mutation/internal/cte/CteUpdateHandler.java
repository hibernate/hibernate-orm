/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.hibernate.query.sqm.tree.cte.SqmCteTable;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 *
 * @author Christian Beikov
 */
public class CteUpdateHandler extends AbstractCteMutationHandler implements UpdateHandler {

	@SuppressWarnings("WeakerAccess")
	public CteUpdateHandler(
			SqmCteTable cteTable,
			SqmUpdateStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			CteStrategy strategy,
			SessionFactoryImplementor sessionFactory) {
		super( cteTable, sqmStatement, domainParameterXref, strategy, sessionFactory );
	}

	@Override
	protected void addDmlCtes(
			CteContainer statement,
			CteStatement idSelectCte,
			MultiTableSqmMutationConverter sqmConverter,
			Map<SqmParameter, List<JdbcParameter>> parameterResolutions,
			SessionFactoryImplementor factory) {
		final TableGroup updatingTableGroup = sqmConverter.getMutatingTableGroup();
		final SqmUpdateStatement<?> updateStatement = (SqmUpdateStatement<?>) getSqmDeleteOrUpdateStatement();
		final EntityMappingType entityDescriptor = getEntityDescriptor();

		final EntityPersister entityPersister = entityDescriptor.getEntityPersister();
		final String rootEntityName = entityPersister.getRootEntityName();
		final EntityPersister rootEntityDescriptor = factory.getDomainModel().getEntityDescriptor( rootEntityName );

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
		final List<Assignment> assignments = new ArrayList<>( setClause.getAssignments().size() );
		final Map<SqmParameter, MappingModelExpressable> paramTypeResolutions = new LinkedHashMap<>();

		sqmConverter.visitSetClause(
				setClause,
				assignments::add,
				(sqmParam, mappingType, jdbcParameters) -> {
					parameterResolutions.put( sqmParam, jdbcParameters );
					paramTypeResolutions.put( sqmParam, mappingType );
				}
		);
		sqmConverter.addVersionedAssignment( assignments::add, updateStatement );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, but the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( updatingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < updatingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( updatingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		final Map<String, List<Assignment>> assignmentsByTable = CollectionHelper.mapOfSize(
				updatingTableGroup.getTableReferenceJoins().size() + 1
		);

		for ( int i = 0; i < assignments.size(); i++ ) {
			final Assignment assignment = assignments.get( i );
			final List<ColumnReference> assignmentColumnRefs = assignment.getAssignable().getColumnReferences();

			String assignmentTableReference = null;

			for ( int c = 0; c < assignmentColumnRefs.size(); c++ ) {
				final ColumnReference columnReference = assignmentColumnRefs.get( c );
				final String tableReference = resolveTableReference(
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

		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableColumnsVisitationSupplier) -> {
					final CteTable dmlResultCte = new CteTable(
							getCteTableName( tableExpression ),
							idSelectCte.getCteTable().getCteColumns(),
							factory
					);
					final List<Assignment> assignmentList;
					if ( updatingTableGroup instanceof UnionTableGroup ) {
						assignmentList = assignmentsByTable.get( updatingTableGroup.getPrimaryTableReference().getTableExpression() );
					}
					else {
						assignmentList = assignmentsByTable.get( tableExpression );
						if ( assignmentList == null ) {
							return;
						}
					}
					final TableReference dmlTableReference = resolveUnionTableReference(
							updatingTableGroup,
							tableExpression
					);
					final List<ColumnReference> columnReferences = new ArrayList<>( idSelectCte.getCteTable().getCteColumns().size() );
					tableColumnsVisitationSupplier.get().accept(
							(index, selectable) -> columnReferences.add(
									new ColumnReference(
											dmlTableReference,
											selectable,
											factory
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

	private String resolveTableReference(
			ColumnReference columnReference,
			Map<String, TableReference> tableReferenceByAlias) {
		final String qualifier = columnReference.getQualifier();
		final TableReference tableReferenceByQualifier = tableReferenceByAlias.get( qualifier );
		if ( tableReferenceByQualifier != null ) {
			return tableReferenceByQualifier.getTableExpression();
		}

		return qualifier;
	}
}
