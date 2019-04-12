/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.SqmIdSelectGenerator;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.spi.Handler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.produce.internal.SqmTreePrinter;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlInsertSelectToJdbcInsertSelectConverter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectInterpretation;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.tree.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcInsertSelect;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

/**
 * Support for {@link Handler} implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableBasedHandler extends AbstractMutationHandler {
	private static final Logger log = Logger.getLogger( AbstractTableBasedHandler.class );

	private final IdTable idTableInfo;
	private final SessionUidSupport sessionUidSupport;
	private final BeforeUseAction beforeUseAction;
	private final AfterUseAction afterUseAction;
	private final IdTableHelper tableHelper;
	private final DomainParameterXref domainParameterXref;


	public AbstractTableBasedHandler(
			SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement,
			IdTable idTableInfo,
			SessionUidSupport sessionUidSupport,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			IdTableHelper idTableHelper,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		super( sqmDeleteOrUpdateStatement, creationContext );
		this.idTableInfo = idTableInfo;
		this.sessionUidSupport = sessionUidSupport;
		this.beforeUseAction = beforeUseAction;
		this.afterUseAction = afterUseAction;

		this.tableHelper = idTableHelper;
		this.domainParameterXref = domainParameterXref;
	}

	public IdTable getIdTableInfo() {
		return idTableInfo;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	public SessionUidSupport getSessionUidSupport() {
		return sessionUidSupport;
	}

	@Override
	public int execute(ExecutionContext executionContext) {

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
	 * @param executionContext
	 */
	protected void beforeExecution(ExecutionContext executionContext) {
	}

	/**
	 * Allow subclasses a chance to perform any clean-up work they need
	 * to perform after execution
	 * @param executionContext
	 */
	protected void afterExecution(ExecutionContext executionContext) {
	}

	protected int performExecution(ExecutionContext executionContext) {
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

	private void performBeforeUseActions(ExecutionContext executionContext) {
		if ( beforeUseAction == BeforeUseAction.CREATE ) {
			tableHelper.createIdTable( executionContext.getSession() );
		}
	}

	private void performAfterUseActions(ExecutionContext executionContext) {
		if ( afterUseAction == AfterUseAction.DROP ) {
			tableHelper.dropIdTable( executionContext.getSession() );
		}
		else if ( afterUseAction == AfterUseAction.CLEAN ) {
			tableHelper.cleanIdTableRows( executionContext.getSession() );
		}
	}

	protected int saveMatchingIdsIntoIdTable(ExecutionContext executionContext) {
		final SqmQuerySpec sqmIdSelectQuerySpec = SqmIdSelectGenerator.generateSqmEntityIdSelect(
				getSqmDeleteOrUpdateStatement(),
				executionContext.getSession().getSessionFactory()
		);

		if ( sessionUidSupport.needsSessionUidColumn() ) {
			// we need to insert the uid into the id-table to properly identify the rows later
			sqmIdSelectQuerySpec.getSelectClause().addSelection(
					new SqmSelection(
							generateSessionUidLiteralExpression( executionContext ),
							executionContext.getSession().getFactory().getNodeBuilder()
					)
			);
		}

		final SqmSelectStatement sqmIdSelect = new SqmSelectStatement(
				executionContext.getSession().getFactory().getNodeBuilder()
		);

		sqmIdSelect.setQuerySpec( sqmIdSelectQuerySpec );

		SqmTreePrinter.logTree( sqmIdSelect );

		final SqmSelectToSqlAstConverter sqmConverter = new SqmSelectToSqlAstConverter(
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				executionContext.getLoadQueryInfluencers(),
				afterLoadAction -> {},
				executionContext.getSession().getSessionFactory()
		);

		final SqmSelectInterpretation sqlAstInterpretation = sqmConverter.interpret( sqmIdSelect );

		final QuerySpec entityIdSelect = sqlAstInterpretation.getSqlAstStatement().getQuerySpec();

		final InsertSelectStatement idTableInsertSelect = generateIdTableInsertSelect(
				idTableInfo,
				entityIdSelect
		);

		final JdbcInsertSelect insertSelectCall = SqlInsertSelectToJdbcInsertSelectConverter.interpret(
				idTableInsertSelect,
				executionContext.getSession().getSessionFactory()
		);

		final JdbcParameterBindings jdbcParameterBindings = QueryHelper.buildJdbcParameterBindings(
				sqmIdSelect, sqlAstInterpretation, executionContext
		);

		return JdbcMutationExecutor.NO_AFTER_STATEMENT_CALL.execute(
				insertSelectCall,
				jdbcParameterBindings,
				executionContext
		);
	}

	private SqmLiteral generateSessionUidLiteralExpression(ExecutionContext executionContext) {
		return new SqmLiteral<>(
				sessionUidSupport.extractUid( executionContext.getSession() ),
				StandardSpiBasicTypes.STRING,
				executionContext.getSession().getFactory().getNodeBuilder()
		);
	}

	public QuerySpec createIdTableSubQuery(ExecutionContext executionContext) {
		final QuerySpec querySpec = new QuerySpec( false );

		final IdTableReference idTableReference = new IdTableReference( getIdTableInfo() );
		final IdTableGroup cteTableGroup = new IdTableGroup( getIdTableInfo().getEntityDescriptor(), idTableReference );
		querySpec.getFromClause().addRoot( cteTableGroup );

		applySelections( querySpec, idTableReference, executionContext );
		applyRestrictions( querySpec, idTableReference, executionContext );

		return querySpec;
	}

	private void applySelections(
			QuerySpec querySpec,
			IdTableReference tableReference,
			ExecutionContext executionContext) {
		int i = 0;
		for ( IdTableColumn column : tableReference.getTable().getIdTableColumns() ) {
			if ( column != tableReference.getTable().getSessionUidColumn() ) {
				querySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								i + 1,
								i,
								tableReference.resolveColumnReference( column ),
								column.getSqlTypeDescriptor().getSqlExpressableType(
										column.getJavaTypeDescriptor(),
										executionContext.getSession().getFactory().getTypeConfiguration()
								)
						)
				);
			}
		}

		return ;
	}

	private void applyRestrictions(
			QuerySpec querySpec,
			IdTableReference idTableReference,
			ExecutionContext executionContext) {
		if ( sessionUidSupport.needsSessionUidColumn() ) {
			querySpec.addRestriction(
					new ComparisonPredicate(
							idTableReference.resolveColumnReference( idTableReference.getTable().getSessionUidColumn() ),
							ComparisonOperator.EQUAL,
							new LiteralParameter(
									sessionUidSupport.extractUid( executionContext.getSession() ),
									StandardSpiBasicTypes.STRING.getSqlExpressableType(),
									Clause.WHERE,
									executionContext.getSession().getFactory().getTypeConfiguration()
							)
					)
			);

		}
	}

	private InsertSelectStatement generateIdTableInsertSelect(
			IdTable idTableInfo,
			QuerySpec entityIdSelect) {
		final InsertSelectStatement insertSelect = new InsertSelectStatement();
		insertSelect.setTargetTable( new IdTableReference( idTableInfo ) );
		insertSelect.setSourceSelectStatement( entityIdSelect );

		// target columns should already be aligned, there should be no need to define them explicitly
		//		via InsertSelectStatement#addTargetColumnReferences

		return insertSelect;
	}

	protected abstract void performMutations(ExecutionContext executionContext);
}
