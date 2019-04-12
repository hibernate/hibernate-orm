/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.internal.DomainParameterXref;
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
import org.hibernate.sql.ast.produce.sqm.internal.SqmUpdateInterpretationImpl;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.domain.AssignableNavigableReference;
import org.hibernate.sql.ast.tree.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement.UpdateStatementBuilder;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmUpdateToSqlAstConverterMultiTable extends BaseSqmToSqlAstConverter {
	private static final Logger log = Logger.getLogger( SqmUpdateToSqlAstConverterMultiTable.class );

	public static SqmUpdateInterpretation interpret(
			SqmUpdateStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			SqlAstCreationContext creationContext) {

		final SqmUpdateToSqlAstConverterMultiTable walker = new SqmUpdateToSqlAstConverterMultiTable(
				sqmStatement,
				idTableSelect,
				queryOptions,
				domainParameterXref,
				domainParameterBindings,
				creationContext
		);

		walker.visitUpdateStatement( sqmStatement );

		final List<UpdateStatement> updateStatements = new ArrayList<>();
		final Set<String> affectedTableNames = new HashSet<>();

		for ( UpdateStatementBuilder builder : walker.updateStatementBuilderMap.values() ) {
			final UpdateStatement sqlAst = builder.createUpdateAst();
			if ( sqlAst != null ) {
				affectedTableNames.add( sqlAst.getTargetTable().getTable().getTableExpression() );
				updateStatements.add( sqlAst );
			}
		}

		return new SqmUpdateInterpretationImpl(
				updateStatements,
				affectedTableNames,
				walker.getJdbcParamsBySqmParam()
		);
	}

	private final EntityTypeDescriptor<?> entityDescriptor;
	private final TableGroup entityTableGroup;


	private final QuerySpec idTableSelect;

	private AssignmentContext currentAssignmentContext;

	private Map<TableReference, UpdateStatementBuilder> updateStatementBuilderMap = new HashMap<>();

	private SqmUpdateToSqlAstConverterMultiTable(
			SqmUpdateStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			SqlAstCreationContext creationContext) {
		super( creationContext, queryOptions, domainParameterXref, domainParameterBindings, LoadQueryInfluencers.NONE, afterLoadAction -> {} );
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
				deleteTarget.getNavigablePath(),
				deleteTarget.getExplicitAlias(),
				JoinType.INNER,
				queryOptions.getLockOptions().getLockMode(),
				this
		);

		getFromClauseIndex().register( deleteTarget, entityTableGroup );

		final TableGroupMock tableGroup = new TableGroupMock( entityTableGroup );
		tableGroup.applyAffectedTableNames( getFromClauseIndex().getAffectedTableNames()::add );

		getFromClauseIndex().register( deleteTarget, tableGroup );

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
	public Object visitUpdateStatement(SqmUpdateStatement sqmStatement) {
		getCurrentClauseStack().push( Clause.UPDATE );

		try {
			for ( SqmAssignment sqmAssignment : sqmStatement.getSetClause().getAssignments() ) {
				currentAssignmentContext = new AssignmentContext( sqmAssignment );

				final AssignableNavigableReference targetReference = (AssignableNavigableReference) sqmAssignment
						.getTargetPath()
						.accept( this );

				assert currentAssignmentContext.tableReference != null;

				// See note in `EmbeddableValuedNavigableReference#applySqlAssignments`..
				//		At this point we know the type of the assignment value (rhs)
				//		because it should be the same as the `targetReference`

				// todo (6.0) : does not yet handle parameters

				targetReference.applySqlAssignments(
						(Expression) sqmAssignment.getValue().accept( this ),
						currentAssignmentContext,
						updateStatementBuilderMap.get( currentAssignmentContext.tableReference )::addAssignment,
						getCreationContext()
				);

				currentAssignmentContext = null;
			}
		}
		finally {
			getCurrentClauseStack().pop();
		}


		return null;
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
			final NavigableReference stateField = (NavigableReference) sqmAssignment.getTargetPath().accept( this );
			currentAssignmentContext.endStateFieldProcessing();

			if ( stateField instanceof AssignableNavigableReference ) {

			}

			final Expression assignedValue = (Expression) sqmAssignment.getValue().accept( this );

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

			final TableReference assignmentTableReference = currentAssignmentContext.tableReference;
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

	private class TableGroupMock extends AbstractTableGroup {
		private final TableGroup entityTableGroup;

		private TableGroupMock(TableGroup entityTableGroup) {
			super(
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
		public TableReference getPrimaryTableReference() {
			return entityTableGroup.locateTableReference(
					( (EntityValuedNavigable<?>) entityTableGroup.getNavigable() ).getEntityDescriptor().getPrimaryTable()
			);
		}

		@Override
		public List<TableReferenceJoin> getTableReferenceJoins() {
			return null;
		}
	}

	public class AssignmentContext {
		private final String assignmentText;
		private boolean processingStateField;

		private TableReference tableReference;

		private AssignmentContext(SqmAssignment assignment) {
			this.assignmentText = assignment.getTargetPath().asLoggableText() + " = " +
					assignment.getValue().asLoggableText();


			log.debugf( "Initializing AssignmentContext [%s]", assignmentText );
		}

		private void injectTableReference(TableReference tableReference) {
			if ( processingStateField ) {
				if ( tableReference != null && tableReference != this.tableReference ) {
					throw new ConversionException(
							String.format(
									Locale.ROOT,
									"Multiple TableReferences found for assignment [%s]",
									assignmentText
							)
					);
				}
			}
			else {
				if ( ! this.tableReference .equals( tableReference ) ) {
					throw new SqlTreeException( "Assignment as part of multi-table update query referenced multiple tables [" + assignmentText + "]" );
				}
			}

			this.tableReference = tableReference;
		}

		private void endStateFieldProcessing() {
			if ( tableReference == null ) {
				throw new SqlTreeException( "Could not determine backing TableReference for assignment state-field" );
			}

			processingStateField = false;
		}

		public TableReference resolveTableReference(Table table) {
			return entityTableGroup.locateTableReference( table );
		}


	}
}
