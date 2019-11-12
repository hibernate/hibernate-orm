/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.SqmIdSelectGenerator;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.spi.Handler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.sql.SqmQuerySpecTranslation;
import org.hibernate.query.sqm.sql.SqmSelectTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstInsertSelectTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.UUIDCharType;

import org.jboss.logging.Logger;

/**
 * Support for {@link Handler} implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableBasedHandler extends AbstractMutationHandler {
	private static final Logger log = Logger.getLogger( AbstractTableBasedHandler.class );

	private final IdTable idTable;
	private final TempTableDdlTransactionHandling ddlTransactionHandling;
	private final BeforeUseAction beforeUseAction;
	private final AfterUseAction afterUseAction;

	private final DomainParameterXref domainParameterXref;

	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;

	private final Supplier<IdTableExporter> exporterSupplier;


	public AbstractTableBasedHandler(
			SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement,
			IdTable idTable,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			DomainParameterXref domainParameterXref,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor,String> sessionUidAccess,
			Supplier<IdTableExporter> exporterSupplier,
			HandlerCreationContext creationContext) {
		super( sqmDeleteOrUpdateStatement, creationContext );
		this.idTable = idTable;
		this.ddlTransactionHandling = ddlTransactionHandling;
		this.beforeUseAction = beforeUseAction;
		this.afterUseAction = afterUseAction;

		this.domainParameterXref = domainParameterXref;

		this.sessionUidAccess = sessionUidAccess;
		this.exporterSupplier = exporterSupplier;
	}

	public IdTable getIdTable() {
		return idTable;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	public BeforeUseAction getBeforeUseAction() {
		return beforeUseAction;
	}

	public AfterUseAction getAfterUseAction() {
		return afterUseAction;
	}

	public Function<SharedSessionContractImplementor, String> getSessionUidAccess() {
		return sessionUidAccess;
	}

	public Supplier<IdTableExporter> getExporterSupplier() {
		return exporterSupplier;
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
	 */
	protected void beforeExecution(ExecutionContext executionContext) {
	}

	/**
	 * Allow subclasses a chance to perform any clean-up work they need
	 * to perform after execution
	 */
	protected void afterExecution(ExecutionContext executionContext) {
	}

	protected int performExecution(ExecutionContext executionContext) {
		performBeforeUseActions( executionContext );

		try {
			// 1) save the matching ids into the id table
			final int affectedRowCount = saveMatchingIdsIntoIdTable( executionContext );
			log.debugf( "insert for matching ids resulted in %s rows", affectedRowCount );

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
		if ( getBeforeUseAction() == BeforeUseAction.CREATE ) {
			IdTableHelper.createIdTable( idTable, getExporterSupplier().get(), ddlTransactionHandling, executionContext.getSession() );
		}
	}

	private void performAfterUseActions(ExecutionContext executionContext) {
		if ( getAfterUseAction() == AfterUseAction.CLEAN ) {
			IdTableHelper.cleanIdTableRows(
					idTable,
					getExporterSupplier().get(),
					sessionUidAccess,
					executionContext.getSession()
			);
		}
		else if ( getAfterUseAction() == AfterUseAction.DROP ) {
			IdTableHelper.dropIdTable(
					idTable,
					getExporterSupplier().get(),
					ddlTransactionHandling,
					executionContext.getSession()
			);
		}
	}

	protected int saveMatchingIdsIntoIdTable(ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final SqmQuerySpec sqmIdSelect = SqmIdSelectGenerator.generateSqmEntityIdSelect(
				getSqmDeleteOrUpdateStatement(),
				executionContext,
				factory
		);

		if ( getIdTable().getSessionUidColumn() != null ) {
			//noinspection unchecked
			sqmIdSelect.getSelectClause().add(
					new SqmSelection(
							new SqmLiteral(
									executionContext.getSession().getSessionIdentifier().toString(),
									UUIDCharType.INSTANCE,
									executionContext.getSession().getFactory().getNodeBuilder()
							),
							null,
							executionContext.getSession().getFactory().getNodeBuilder()
					)
			);
		}

		final SqmTranslatorFactory sqmTranslatorFactory = factory.getQueryEngine().getSqmTranslatorFactory();
		final SqmSelectTranslator sqmTranslator = sqmTranslatorFactory.createSelectTranslator(
				QueryOptions.NONE,
				domainParameterXref,
				executionContext.getQueryParameterBindings(),
				executionContext.getSession().getLoadQueryInfluencers(),
				factory
		);

		final SqmQuerySpecTranslation sqmIdSelectTranslation = sqmTranslator.translate( sqmIdSelect );

		final InsertSelectStatement insertSelectStatement = new InsertSelectStatement();

		final TableReference idTableReference = new TableReference( idTable.getTableExpression(), null, false, factory );

		insertSelectStatement.setTargetTable( idTableReference );
		insertSelectStatement.setSourceSelectStatement( sqmIdSelectTranslation.getSqlAst() );

		for ( int i = 0; i < idTable.getIdTableColumns().size(); i++ ) {
			final IdTableColumn column = idTable.getIdTableColumns().get( i );
			insertSelectStatement.addTargetColumnReferences(
					new ColumnReference( idTableReference, column.getColumnName(), column.getJdbcMapping(), factory )
			);
		}

		final JdbcServices jdbcServices = factory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		final SqlAstInsertSelectTranslator sqlAstTranslator = sqlAstTranslatorFactory.buildInsertTranslator( factory );
		final JdbcInsert jdbcInsert = sqlAstTranslator.translate( insertSelectStatement );


		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						sqmIdSelectTranslation::getJdbcParamsBySqmParam
				),
				sqmTranslator,
				executionContext.getSession()
		);

		return jdbcServices.getJdbcInsertExecutor().execute(
				jdbcInsert,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {
				},
				executionContext
		);
	}


	public QuerySpec createIdTableSubQuery(ExecutionContext executionContext) {
		final QuerySpec querySpec = new QuerySpec( false );

		final TableReference idTableReference = new TableReference(
				idTable.getTableExpression(),
				null,
				true,
				executionContext.getSession().getFactory()
		);
		final TableGroup idTableGroup = new StandardTableGroup(
				new NavigablePath( idTableReference.getTableExpression() ),
				getEntityDescriptor(),
				LockMode.NONE,
				idTableReference,
				Collections.emptyList(),
				null,
				executionContext.getSession().getFactory()
		);

		querySpec.getFromClause().addRoot( idTableGroup );

		applySelections( querySpec, idTableReference, executionContext );
		applyRestrictions( querySpec, idTableReference, executionContext );

		return querySpec;
	}

	private void applySelections(
			QuerySpec querySpec,
			TableReference tableReference,
			ExecutionContext executionContext) {
		for ( int i = 0; i < idTable.getIdTableColumns().size(); i++ ) {
			final IdTableColumn idTableColumn = idTable.getIdTableColumns().get( i );
			if ( idTableColumn != idTable.getSessionUidColumn() ) {
				querySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								i+1,
								i,
								new ColumnReference(
										tableReference,
										idTableColumn.getColumnName(),
										idTableColumn.getJdbcMapping(),
										executionContext.getSession().getFactory()
								),
								idTableColumn.getJdbcMapping()
						)
				);
			}
		}
	}

	private void applyRestrictions(
			QuerySpec querySpec,
			TableReference idTableReference,
			ExecutionContext executionContext) {
		if ( idTable.getSessionUidColumn() != null ) {
			querySpec.applyPredicate(
					new ComparisonPredicate(
							new ColumnReference(
									idTableReference,
									idTable.getSessionUidColumn().getColumnName(),
									idTable.getSessionUidColumn().getJdbcMapping(),
									executionContext.getSession().getFactory()
							),
							ComparisonOperator.EQUAL,
							new QueryLiteral(
									sessionUidAccess.apply( executionContext.getSession() ),
									UUIDCharType.INSTANCE,
									Clause.WHERE
							)
					)
			);
		}
	}

	protected abstract void performMutations(ExecutionContext executionContext);
}
