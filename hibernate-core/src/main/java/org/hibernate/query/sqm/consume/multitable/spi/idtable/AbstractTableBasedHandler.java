/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.sqm.consume.multitable.spi.Handler;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerCreationContext;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerExecutionContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.consume.spi.SqlInsertSelectToJdbcInsertSelectConverter;
import org.hibernate.sql.ast.produce.sqm.internal.IdSelectGenerator;
import org.hibernate.sql.ast.tree.spi.InsertSelectStatement;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.exec.spi.JdbcInsertSelect;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Support for {@link Handler} implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableBasedHandler implements Handler {
	private static final Logger log = Logger.getLogger( AbstractTableBasedHandler.class );

	private final SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement;
	private final EntityTypeDescriptor entityDescriptor;
	private final IdTable idTableInfo;
	private final SessionUidSupport sessionUidSupport;
	private final BeforeUseAction beforeUseAction;
	private final AfterUseAction afterUseAction;
	private final HandlerCreationContext creationContext;
	private final IdTableHelper tableHelper;


	public AbstractTableBasedHandler(
			SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement,
			EntityTypeDescriptor entityDescriptor,
			IdTable idTableInfo,
			SessionUidSupport sessionUidSupport,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			IdTableHelper idTableHelper,
			HandlerCreationContext creationContext) {
		this.sqmDeleteOrUpdateStatement = sqmDeleteOrUpdateStatement;
		this.entityDescriptor = entityDescriptor;
		this.idTableInfo = idTableInfo;
		this.sessionUidSupport = sessionUidSupport;
		this.beforeUseAction = beforeUseAction;
		this.afterUseAction = afterUseAction;
		this.creationContext = creationContext;

		this.tableHelper = idTableHelper;
	}

	public EntityTypeDescriptor<?> getEntityDescriptor() {
		return entityDescriptor;
	}

	public IdTable getIdTableInfo() {
		return idTableInfo;
	}

	public SqmDeleteOrUpdateStatement getSqmDeleteOrUpdateStatement() {
		return sqmDeleteOrUpdateStatement;
	}

	public HandlerCreationContext getCreationContext() {
		return creationContext;
	}

	@Override
	public int execute(HandlerExecutionContext executionContext) {

		// In general:
		//		1) prepare for use - this is completely a subclass hook
		//		2) perform execution
		//		3) release after use - again, completely a subclass hook

		beforeExecution( executionContext );

		try {
			return performExecution( executionContext );
		}
		finally {
			afterExecution( executionContext );
		}
	}

	/**
	 * Allow subclasses a chance to perform any preliminary work they need
	 * to perform prior to execution
	 */
	protected void beforeExecution(HandlerExecutionContext executionContext) {
	}

	/**
	 * Allow subclasses a chance to perform any clean-up work they need
	 * to perform after execution
	 */
	protected void afterExecution(HandlerExecutionContext executionContext) {
	}

	protected int performExecution(HandlerExecutionContext executionContext) {
		performBeforeUseActions( executionContext );

		try {
			// 1) save the matching ids into the id table
			final int affectedRowCount = saveMatchingIdsIntoIdTable( executionContext );

			// 2) perform the actual individual update or deletes, using
			// 		inclusion in the id-table as restriction
			performMutations( executionContext );

			return affectedRowCount;
		}
		finally {
			performAfterUseActions( executionContext );
		}
	}

	private void performBeforeUseActions(HandlerExecutionContext executionContext) {
		if ( beforeUseAction == BeforeUseAction.CREATE ) {
			tableHelper.createIdTable( executionContext.getSession() );
		}
	}

	private void performAfterUseActions(HandlerExecutionContext executionContext) {
		if ( afterUseAction == AfterUseAction.DROP ) {
			tableHelper.dropIdTable( executionContext.getSession() );
		}
		else if ( afterUseAction == AfterUseAction.CLEAN ) {
			tableHelper.cleanIdTableRows( executionContext.getSession() );
		}
	}

	protected int saveMatchingIdsIntoIdTable(HandlerExecutionContext executionContext) {
		final QuerySpec entityIdSelect = generateEntityIdSelect(
				entityDescriptor,
				sqmDeleteOrUpdateStatement,
				executionContext
		);

		if ( sessionUidSupport.needsSessionUidColumn() ) {
			final QueryLiteral sessUidLiteral = generateSessionUidLiteralExpression( executionContext );
			final TypeConfiguration typeConfiguration = executionContext.getSessionFactory().getTypeConfiguration();

			// we need to insert the uid into the id-table to properly identify the rows later
			final int selectionCountSoFar = entityIdSelect.getSelectClause().getSqlSelections().size();
			entityIdSelect.getSelectClause().addSqlSelection(
					sessUidLiteral.createSqlSelection(
							selectionCountSoFar + 1,
							selectionCountSoFar,
							(BasicJavaDescriptor) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( String.class ),
							typeConfiguration
					)
			);
		}

		final InsertSelectStatement idTableInsertSelectAst = generateIdTableInsertSelect(
				idTableInfo,
				entityIdSelect
		);

		final JdbcInsertSelect insertSelectCall = SqlInsertSelectToJdbcInsertSelectConverter.interpret(
				idTableInsertSelectAst,
				executionContext.getSessionFactory()
		);

		return JdbcMutationExecutor.NO_AFTER_STATEMENT_CALL.execute(
				insertSelectCall,
				executionContext,
				Connection::prepareStatement
		);
	}

	private QueryLiteral generateSessionUidLiteralExpression(HandlerExecutionContext executionContext) {
		return new QueryLiteral(
				sessionUidSupport.extractUid( executionContext.getSession() ),
				StandardSpiBasicTypes.STRING.getSqlExpressableType( executionContext.getSessionFactory().getTypeConfiguration() ),
				Clause.IRRELEVANT
		);
	}

	protected static QuerySpec generateEntityIdSelect(
			EntityTypeDescriptor entityDescriptor,
			SqmDeleteOrUpdateStatement sqmUpdateStatement,
			HandlerExecutionContext executionContext) {
		// todo (6.0) : we are parsing the SQM multiple times here:
		//		1) generateEntityIdSelect
		//		2) generateIdTableInsertSelect
		return IdSelectGenerator.generateEntityIdSelect(
				entityDescriptor,
				sqmUpdateStatement,
				executionContext.getQueryOptions(),
				executionContext.getLoadQueryInfluencers(),
				executionContext.getSession().getFactory()
		);
	}

	private InsertSelectStatement generateIdTableInsertSelect(
			IdTable idTableInfo,
			QuerySpec entityIdSelect) {
		final InsertSelectStatement insertSelect = new InsertSelectStatement();
		insertSelect.setTargetTable( new IdTableReference( idTableInfo, null ) );
		insertSelect.setSourceSelectStatement( entityIdSelect );

		// target columns should already be aligned, there should be no need to define them explicitly
		//		via InsertSelectStatement#addTargetColumnReferences

		return insertSelect;
	}

	protected abstract void performMutations(HandlerExecutionContext executionContext);

	protected QuerySpec generateIdTableSelect(HandlerExecutionContext executionContext) {
		QuerySpec idTableSelect = new QuerySpec( false );
		final TableSpace tableSpace = idTableSelect.getFromClause().makeTableSpace();
		tableSpace.setRootTableGroup( createTableGroupForIdTable( idTableInfo, tableSpace ) );

		Collection<Column> columns = idTableInfo.getColumns();
		columns.forEach( column -> {
			SqlSelection sqlSelection = new SqlSelectionImpl(
					0,
					0,
					new ColumnReference( new TableReference( idTableInfo, null, false ), column ),
					column.getExpressableType().getJdbcValueExtractor()
			);
			idTableSelect.getSelectClause().addSqlSelection( sqlSelection );
		} );

		// account for session uid column in the id table, if one
		if ( sessionUidSupport.needsSessionUidColumn() ) {
			final IdTableColumn sessUidColumn = (IdTableColumn) idTableInfo.getColumn( SessionUidSupport.SESSION_ID_COLUMN_NAME );
			idTableSelect.addRestriction(
					new RelationalPredicate(
							RelationalPredicate.Operator.EQUAL,
							new ColumnReference(
									tableSpace.getRootTableGroup(),
									sessUidColumn
							),
							generateSessionUidLiteralExpression( executionContext )
					)
			);
		}

		return idTableSelect;
	}

	private TableGroup createTableGroupForIdTable(
			IdTable idTableInfo,
			TableSpace tableSpace) {
		return new IdTableGroup( tableSpace, new IdTableReference( idTableInfo, null ) );
	}

	private static class IdTableGroup extends AbstractTableGroup {
		private final IdTableReference idTableReference;

		public IdTableGroup(
				TableSpace tableSpace,
				IdTableReference idTableReference) {
			super( tableSpace, "id_table" );
			this.idTableReference = idTableReference;
		}

		@Override
		protected TableReference getPrimaryTableReference() {
			return idTableReference;
		}

		@Override
		protected List<TableReferenceJoin> getTableReferenceJoins() {
			return Collections.emptyList();
		}

		@Override
		public NavigableReference getNavigableReference() {
			throw new UnsupportedOperationException( "IdTable cannot be used as an Expression" );
		}

		@Override
		public void render(SqlAppender sqlAppender, SqlAstWalker walker) {
			renderTableReference( getPrimaryTableReference(), sqlAppender, walker );
		}
	}
}
