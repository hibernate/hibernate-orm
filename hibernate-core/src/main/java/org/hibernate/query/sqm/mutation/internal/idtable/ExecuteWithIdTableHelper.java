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
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmTreePrinter;
import org.hibernate.query.sqm.mutation.internal.SqmIdSelectGenerator;
import org.hibernate.query.sqm.sql.SqmQuerySpecTranslation;
import org.hibernate.query.sqm.sql.SqmSelectTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
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
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.UUIDCharType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public final class ExecuteWithIdTableHelper {
	private static final Logger log = Logger.getLogger( ExecuteWithIdTableHelper.class );
	public static final boolean debugging = log.isDebugEnabled();

	private ExecuteWithIdTableHelper() {
	}

	public static int saveMatchingIdsIntoIdTable(
			SqmUpdateStatement sqmMutation,
			MultiTableSqmMutationConverter sqmConverter,
			TableGroup mutatingTableGroup,
			Predicate suppliedPredicate,
			IdTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final SqmQuerySpec sqmIdSelect = SqmIdSelectGenerator.generateSqmEntityIdSelect(
				sqmMutation,
				factory
		);

		if ( idTable.getSessionUidColumn() != null ) {
			//noinspection unchecked
			sqmIdSelect.getSelectClause().add(
					new SqmSelection(
							new SqmLiteral(
									sessionUidAccess.apply( executionContext.getSession() ),
									UUIDCharType.INSTANCE,
									executionContext.getSession().getFactory().getNodeBuilder()
							),
							null,
							executionContext.getSession().getFactory().getNodeBuilder()
					)
			);
		}

		SqmTreePrinter.logTree( sqmIdSelect, "Entity-identifier Selection SqmQuerySpec" );

		final InsertSelectStatement insertSelectStatement = new InsertSelectStatement();

		final TableReference idTableReference = new TableReference( idTable.getTableExpression(), null, false, factory );
		insertSelectStatement.setTargetTable( idTableReference );

		final QuerySpec matchingIdRestrictionQuerySpec = generateTempTableInsertValuesQuerySpec(
				sqmConverter,
				mutatingTableGroup,
				suppliedPredicate,
				sqmIdSelect
		);
		insertSelectStatement.setSourceSelectStatement( matchingIdRestrictionQuerySpec );

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

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcInsert,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}

	private static QuerySpec generateTempTableInsertValuesQuerySpec(
			MultiTableSqmMutationConverter sqmConverter,
			TableGroup mutatingTableGroup, Predicate suppliedPredicate, SqmQuerySpec sqmIdSelect) {
		final QuerySpec matchingIdRestrictionQuerySpec = new QuerySpec( false, 1 );
		sqmConverter.visitSelectClause(
				sqmIdSelect.getSelectClause(),
				matchingIdRestrictionQuerySpec,
				columnReference -> {},
				(sqmParameter, jdbcParameters) -> {}
		);
		matchingIdRestrictionQuerySpec.getFromClause().addRoot( mutatingTableGroup );
		matchingIdRestrictionQuerySpec.applyPredicate( suppliedPredicate );
		return matchingIdRestrictionQuerySpec;
	}

	public static int saveMatchingIdsIntoIdTable(
			SqmDeleteOrUpdateStatement sqmMutation,
			Predicate predicate,
			IdTable idTable,
			Function<SharedSessionContractImplementor,String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final SqmQuerySpec sqmIdSelect = SqmIdSelectGenerator.generateSqmEntityIdSelect(
				sqmMutation,
				factory
		);

		if ( idTable.getSessionUidColumn() != null ) {
			//noinspection unchecked
			sqmIdSelect.getSelectClause().add(
					new SqmSelection(
							new SqmLiteral(
									sessionUidAccess.apply( executionContext.getSession() ),
									UUIDCharType.INSTANCE,
									executionContext.getSession().getFactory().getNodeBuilder()
							),
							null,
							executionContext.getSession().getFactory().getNodeBuilder()
					)
			);
		}

		SqmTreePrinter.logTree( sqmIdSelect, "Entity-identifier Selection SqmQuerySpec" );

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

		final QuerySpec matchingIdRestrictionQuerySpec = sqmIdSelectTranslation.getSqlAst();
		insertSelectStatement.setSourceSelectStatement( matchingIdRestrictionQuerySpec );
		matchingIdRestrictionQuerySpec.applyPredicate( predicate );

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

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcInsert,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}

	public static QuerySpec createIdTableSelectQuerySpec(
			IdTable idTable,
			Function<SharedSessionContractImplementor,String> sessionUidAccess,
			EntityMappingType entityDescriptor,
			ExecutionContext executionContext) {
		final QuerySpec querySpec = new QuerySpec( false );

		final TableReference idTableReference = new TableReference(
				idTable.getTableExpression(),
				null,
				true,
				executionContext.getSession().getFactory()
		);
		final TableGroup idTableGroup = new StandardTableGroup(
				new NavigablePath( idTableReference.getTableExpression() ),
				entityDescriptor,
				LockMode.NONE,
				idTableReference,
				Collections.emptyList(),
				null,
				executionContext.getSession().getFactory()
		);

		querySpec.getFromClause().addRoot( idTableGroup );

		applyIdTableSelections( querySpec, idTableReference, idTable, executionContext );
		applyIdTableRestrictions( querySpec, idTableReference, idTable, sessionUidAccess, executionContext );

		return querySpec;
	}

	private static void applyIdTableSelections(
			QuerySpec querySpec,
			TableReference tableReference,
			IdTable idTable,
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

	private static void applyIdTableRestrictions(
			QuerySpec querySpec,
			TableReference idTableReference,
			IdTable idTable,
			Function<SharedSessionContractImplementor,String> sessionUidAccess,
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

	public static void performBeforeIdTableUseActions(
			BeforeUseAction beforeUseAction,
			IdTable idTable,
			Supplier<IdTableExporter> idTableExporterAccess,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			ExecutionContext executionContext) {
		if ( beforeUseAction == BeforeUseAction.CREATE ) {
			IdTableHelper.createIdTable(
					idTable,
					idTableExporterAccess.get(),
					ddlTransactionHandling,
					executionContext.getSession()
			);
		}
	}

	public static void performAfterIdTableUseActions(
			AfterUseAction afterUseAction,
			IdTable idTable,
			Supplier<IdTableExporter> idTableExporterAccess,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			ExecutionContext executionContext) {
		if ( afterUseAction == AfterUseAction.CLEAN ) {
			IdTableHelper.cleanIdTableRows(
					idTable,
					idTableExporterAccess.get(),
					sessionUidAccess,
					executionContext.getSession()
			);
		}
		else if ( afterUseAction == AfterUseAction.DROP ) {
			IdTableHelper.dropIdTable(
					idTable,
					idTableExporterAccess.get(),
					ddlTransactionHandling,
					executionContext.getSession()
			);
		}
	}
}
