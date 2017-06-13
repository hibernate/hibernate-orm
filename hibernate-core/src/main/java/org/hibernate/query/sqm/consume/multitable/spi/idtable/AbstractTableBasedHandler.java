/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sqm.consume.multitable.spi.Handler;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerCreationContext;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerExecutionContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.sql.ast.consume.spi.JdbcInsertSelect;
import org.hibernate.sql.ast.consume.spi.JdbcMutationExecutor;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.consume.spi.SqlInsertSelectToJdbcInsertSelectConverter;
import org.hibernate.sql.ast.produce.sqm.internal.IdSelectGenerator;
import org.hibernate.sql.ast.tree.spi.InsertSelectStatement;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;

import org.jboss.logging.Logger;

/**
 * Support for {@link Handler} implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableBasedHandler implements Handler {
	private static final Logger log = Logger.getLogger( AbstractTableBasedHandler.class );

	private final SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement;
	private final EntityDescriptor entityDescriptor;
	private final IdTableSupport idTableSupport;
	private final IdTable idTableInfo;
	private final HandlerCreationContext creationContext;

	/**
	 * The creation command for the id-table
	 */
	private String idTableCreationCommand;


	public AbstractTableBasedHandler(
			SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement,
			EntityDescriptor entityDescriptor,
			IdTableSupport idTableSupport,
			IdTable idTableInfo,
			HandlerCreationContext creationContext) {
		this.sqmDeleteOrUpdateStatement = sqmDeleteOrUpdateStatement;
		this.entityDescriptor = entityDescriptor;
		this.idTableSupport = idTableSupport;
		this.idTableInfo = idTableInfo;
		this.creationContext = creationContext;
	}

	public EntityDescriptor getEntityDescriptor() {
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

	protected void createIdTable(
			IdTableInfo idTableInfo,
			JdbcServices jdbcServices,
			JdbcConnectionAccess jdbcConnectionAccess) {
		// this may be called multiple times for certain strategies, so we want to
		//		cache them in case we need it multiple times.  but don't build it
		//		until we are sure we will need it
		if ( idTableCreationCommand == null ) {
			idTableCreationCommand = generateIdTableCreationCommand( idTableInfo, jdbcServices );
		}

		// todo (6.0) : handle transaction-ality
		//
		//		this is a 3-way decision tree including:
		//			1) should the creation be transacted or not
		//			2) should we join or suspend an existing transaction
		//			3) does

		// btw this applies to creates and drops...

		//		this is best viewed as a matrix like:
		//
		//					transacted     not-transacted
		//		----------|--------------|---------------
		//
		//
		//		- do we stop any current transaction?  or join with it?
		//		- does there need to be a transaction?
		//
		//		in general, options are:
		//			-
		try {
			final Connection connection = jdbcConnectionAccess.obtainConnection();

			try {
				try (Statement statement = connection.createStatement()) {
					statement.execute( idTableCreationCommand );
				}
			}
			finally {
				try {
					connection.close();
				}
				catch (Exception ignore) {
				}
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					String.format(
							Locale.ROOT,
							"Could not perform id-table creation [%s] using strategy %s",
							idTableCreationCommand,
							getClass().getName()
					)
			);
		}
	}

	private String generateIdTableCreationCommand(IdTableInfo idTableInfo, JdbcServices jdbcServices) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final String qualifiedNameText = jdbcEnvironment.getQualifiedObjectNameFormatter()
				.format( idTableInfo.getName(), dialect );

		log.debugf( "About to create id-table %s", qualifiedNameText );

		final StringBuilder sqlBuffer = new StringBuilder( idTableSupport.getCreateIdTableCommand() )
				.append( ' ' )
				.append( qualifiedNameText )
				.append( " (" );

		boolean firstPass = true;
		for ( ColumnInfo column : idTableInfo.getColumns() ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				// append a comma and space to separate from the previous column
				sqlBuffer.append( ", " );
			}

			final String columnName = jdbcEnvironment.getIdentifierHelper()
					.toMetaDataObjectName( column.getName() );

			final String sqlTypeDescription = jdbcEnvironment.getDialect()
					.getTypeName( column.getSqlTypeDescriptor().getJdbcTypeCode() );

			sqlBuffer.append( columnName )
					.append( ' ' )
					.append( sqlTypeDescription );
		}

		sqlBuffer.append( ")" );

		if ( idTableSupport.getCreateIdTableStatementOptions() != null ) {
			sqlBuffer.append( ' ' )
					.append( idTableSupport.getCreateIdTableStatementOptions() );
		}

		return sqlBuffer.toString();
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
		// 1) save the matching ids into the id table
		final int affectedRowCount = saveMatchingIdsIntoIdTable( executionContext );

		// 2) perform the actual individual update or deletes, using
		// 		inclusion in the id-table as restriction
		performMutations( executionContext );

		return affectedRowCount;
	}

	protected int saveMatchingIdsIntoIdTable(HandlerExecutionContext executionContext) {
		final QuerySpec entityIdSelect = generateEntityIdSelect(
				entityDescriptor,
				sqmDeleteOrUpdateStatement,
				executionContext
		);

		final InsertSelectStatement idTableInsertSelectAst = generateIdTableInsertSelect(
				idTableInfo,
				entityIdSelect
		);

		final JdbcInsertSelect insertSelectCall = SqlInsertSelectToJdbcInsertSelectConverter.interpret(
				idTableInsertSelectAst,
				executionContext
		);

		return JdbcMutationExecutor.NO_AFTER_STATEMENT_CALL.execute(
				insertSelectCall,
				executionContext.getQueryOptions(),
				Connection::prepareStatement,
				executionContext.getParameterBindingContext(),
				afterLoadAction -> {},
				executionContext.getSession()
		);
	}

	protected static QuerySpec generateEntityIdSelect(
			EntityDescriptor entityDescriptor,
			SqmDeleteOrUpdateStatement sqmUpdateStatement,
			HandlerExecutionContext executionContext) {
		return IdSelectGenerator.generateEntityIdSelect(
				entityDescriptor,
				sqmUpdateStatement,
				executionContext.getQueryOptions(),
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

	protected QuerySpec generateIdTableSelect() {
		QuerySpec idTableSelect = new QuerySpec( false );
		final TableSpace tableSpace = idTableSelect.getFromClause().makeTableSpace();
		tableSpace.setRootTableGroup( createTableGroupForIdTable( idTableInfo, tableSpace ) );

		// todo (6.0) : still need to add SqlSelections

		// todo (6.0) : still need to account for persistent tables in terms of adding Session-uid column restriction
		//		and add a ParameterBinder for that restriction parameter.

		return idTableSelect;
	}

	private TableGroup createTableGroupForIdTable(
			IdTable idTableInfo,
			TableSpace tableSpace) {
		return new AbstractTableGroup( tableSpace, "id_table") {
			final IdTableReference idTableReference = new IdTableReference( idTableInfo, null );
			@Override
			protected TableReference getPrimaryTableReference() {
				return idTableReference;
			}

			@Override
			protected List<TableReferenceJoin> getTableReferenceJoins() {
				return Collections.emptyList();
			}

			@Override
			public NavigableReference asExpression() {
				throw new UnsupportedOperationException( "IdTable cannot be used as an Expression" );
			}

			@Override
			public void render(SqlAppender sqlAppender, SqlAstWalker walker) {
				renderTableReference( getPrimaryTableReference(), sqlAppender, walker );
			}
		};
	}
}
