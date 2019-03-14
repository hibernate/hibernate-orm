/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.SqlTreeException;
import org.hibernate.sql.ast.produce.internal.SqlAstProcessingStateImpl;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.UpdateStatement.UpdateStatementBuilder;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.InSubQueryPredicate;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmUpdateToSqlAstConverterMultiTable extends BaseSqmToSqlAstConverter {
	private static final Logger log = Logger.getLogger( SqmUpdateToSqlAstConverterMultiTable.class );


	public static List<SqlAstUpdateDescriptor> interpret(
			SqmUpdateStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			SqlAstCreationContext creationContext) {

		final SqmUpdateToSqlAstConverterMultiTable walker = new SqmUpdateToSqlAstConverterMultiTable(
				sqmStatement,
				idTableSelect,
				queryOptions,
				creationContext
		);

		walker.visitUpdateStatement( sqmStatement );

		return walker.updateStatementBuilderMap.entrySet().stream()
				.map( entry -> entry.getValue().createUpdateDescriptor() )
				.collect( Collectors.toList() );
	}

	private final EntityTypeDescriptor entityDescriptor;
	private final TableGroup entityTableGroup;

	private final QuerySpec idTableSelect;

	private AssignmentContext currentAssignmentContext;

	private Map<TableReference, UpdateStatementBuilder> updateStatementBuilderMap = new HashMap<>();

	private SqmUpdateToSqlAstConverterMultiTable(
			SqmUpdateStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			SqlAstCreationContext creationContext) {
		super( creationContext, queryOptions, LoadQueryInfluencers.NONE, afterLoadAction -> {} );
		this.idTableSelect = idTableSelect;

		final SqmRoot deleteTarget = sqmStatement.getTarget();
		this.entityDescriptor = deleteTarget.getReferencedNavigable().getEntityDescriptor();

		// Ask the entity descriptor to create a TableGroup.  This TableGroup
		//		will contain all the individual TableReferences we need..
		//
		// here though we will need to "put into buckets" all the assignments that
		// 		need to be executed against a particular table in the group.  this
		//		should be doable based on a custom SqlAstWalker converter for update
		//		statements which keeps track of the table of the attribute being assigned
		//		in the query so that we know some context for these assigned values.


		this.entityTableGroup = entityDescriptor.createRootTableGroup(
				deleteTarget.getUniqueIdentifier(),
				deleteTarget.getNavigablePath(),
				deleteTarget.getExplicitAlias(),
				JoinType.INNER,
				queryOptions.getLockOptions().getLockMode(),
				this
		);

		getFromClauseIndex().crossReference( deleteTarget, entityTableGroup );

		final TableGroupMock tableGroup = new TableGroupMock( entityTableGroup );
		tableGroup.applyAffectedTableNames( affectedTableNames()::add );

		primeStack( getTableGroupStack(), tableGroup );
		getFromClauseIndex().crossReference( deleteTarget, tableGroup );

		getProcessingStateStack().push(
				new SqlAstProcessingStateImpl(
						null,
						this,
						getCurrentClauseStack()::getCurrent,
						() -> (expr) -> {}
				)
		);
	}

	@Override
	public Object visitSetClause(SqmSetClause setClause) {
		getCurrentClauseStack().push( Clause.UPDATE );
		try {
			return super.visitSetClause( setClause );
		}
		finally {
			getCurrentClauseStack().pop();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Assignment visitAssignment(SqmAssignment sqmAssignment) {
		currentAssignmentContext = new AssignmentContext( sqmAssignment );

		try {
			final NavigableReference stateField = (NavigableReference) sqmAssignment.getStateField().accept( this );
			currentAssignmentContext.endStateFieldProcessing();
			final Expression assignedValue = (Expression) sqmAssignment.getStateField().accept( this );

			// todo (6.0) : consider SingularAttributeReference#generateSqlAssignments returning a List of Assignment references
			//		representing the update of its (updatable) columns
			// todo (6.0) : Do we want to allow updates against embedded values as well?
			// 		- JPA defines support for updates only against basic (single-column) types,
			//		this would be an "extension" feature as well as needing to be one of the "strict compliance checks"

			// todo (6.0) : this also illustraes why being able to ask the Navigable for its list of (Sql)Expressions, rather than just SqlSelections, is important
			//		in fact its probably a good idea to have Navigable be the place where we build all
			// 		SqlNode references (TableGroups, Expressions, Assignments, etc

//			final SqlSelectionGroup sqlSelectionGroup = stateField.getNavigable().resolveSqlSelectionGroup(
//					entityTableGroup,
//					this
//			);
//			assert sqlSelectionGroup.getSqlSelections().size() == 1;

//			final Assignment assignment = new Assignment( stateField, assignedValue, this );
			final Assignment assignment = null;

			final TableReference assignmentTableReference = currentAssignmentContext.stateFieldTableReference;
			UpdateStatementBuilder concreteUpdateStatementBuilder = updateStatementBuilderMap.get( assignmentTableReference );
			if ( concreteUpdateStatementBuilder == null ) {
				concreteUpdateStatementBuilder = new UpdateStatementBuilder( assignmentTableReference );
				concreteUpdateStatementBuilder.addRestriction(
						new InSubQueryPredicate(
								null,
								idTableSelect,
								false
						)
				);
				updateStatementBuilderMap.put( assignmentTableReference, concreteUpdateStatementBuilder );
			}
			concreteUpdateStatementBuilder.addAssignment( assignment );

			return assignment;
		}
		finally {
			currentAssignmentContext = null;
		}
	}

	private class TableGroupMock extends AbstractTableGroup implements TableGroup {
		private final TableGroup entityTableGroup;

		private TableGroupMock(TableGroup entityTableGroup) {
			super(
					entityTableGroup.getUniqueIdentifier(),
					entityTableGroup.getNavigablePath(),
					entityTableGroup.getNavigable(),
					entityTableGroup.getLockMode()
			);
			this.entityTableGroup = entityTableGroup;
		}

		@Override
		public TableReference locateTableReference(Table table) {
			final TableReference tableReference = entityTableGroup.locateTableReference( table );

			if ( currentAssignmentContext != null ) {
				currentAssignmentContext.injectTableReference( tableReference );
			}

			return tableReference;
		}

		@Override
		public Column resolveColumn(String columnName) {
			return entityTableGroup.resolveColumn( columnName );
		}

		@Override
		public void render(SqlAppender sqlAppender, SqlAstWalker walker) {

		}

		@Override
		public void applyAffectedTableNames(Consumer<String> nameCollector) {
			entityTableGroup.applyAffectedTableNames( nameCollector );
		}

		@Override
		protected TableReference getPrimaryTableReference() {
			return entityTableGroup.locateTableReference(
					( (EntityValuedNavigable<?>) entityTableGroup.getNavigable() ).getEntityDescriptor().getPrimaryTable()
			);
		}

		@Override
		protected List<TableReferenceJoin> getTableReferenceJoins() {
			return null;
		}
	}

	private static class AssignmentContext {
		private final String assignmentText;
		private boolean processingStateField;

		private TableReference stateFieldTableReference;

		private AssignmentContext(SqmAssignment assignment) {
			this.assignmentText = assignment.getStateField().asLoggableText() + " = " +
					assignment.getValue().asLoggableText();

			log.debugf( "Initializing AssignmentContext [%s]", assignmentText );
		}

		private void injectTableReference(TableReference tableReference) {
			if ( processingStateField ) {
				if ( stateFieldTableReference != null ) {
					throw new ConversionException( "Multiple TableReferences found for assignment state-field" );
				}
				else {
					stateFieldTableReference = tableReference;
				}
			}
			else {
				// `stateFieldTableReference != null` already validated during `endStateFieldProcessing`
				if ( !stateFieldTableReference.equals( tableReference ) ) {
					throw new SqlTreeException( "Assignment as part of multi-table update query referenced multiple tables [" + assignmentText + "]" );
				}
				this.stateFieldTableReference = tableReference;
			}
		}

		private void endStateFieldProcessing() {
			if ( stateFieldTableReference == null ) {
				throw new SqlTreeException( "Could not determine backing TableReference for assignment state-field" );
			}

			processingStateField = false;
		}
	}
}
