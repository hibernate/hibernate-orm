/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.sql.Connection;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerCreationContext;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerExecutionContext;
import org.hibernate.query.sqm.consume.multitable.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.sql.ast.consume.spi.JdbcMutationExecutor;
import org.hibernate.sql.ast.consume.spi.JdbcUpdate;
import org.hibernate.sql.ast.consume.spi.SqlUpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmUpdateToSqlAstConverterMultiTable;
import org.hibernate.sql.ast.tree.spi.UpdateStatement;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
public class TableBasedUpdateHandlerImpl
		extends AbstractTableBasedHandler
		implements UpdateHandler, SqlAstBuildingContext {
	private static final Logger log = Logger.getLogger( TableBasedUpdateHandlerImpl.class );

	public TableBasedUpdateHandlerImpl(
			SqmUpdateStatement sqmUpdateStatement,
			EntityDescriptor entityDescriptor,
			IdTableSupport idTableSupport,
			IdTable idTableInfo,
			HandlerCreationContext creationContext) {
		super( sqmUpdateStatement, entityDescriptor, idTableSupport, idTableInfo, creationContext );
	}

	@Override
	public SqmUpdateStatement getSqmDeleteOrUpdateStatement() {
		return (SqmUpdateStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	protected void performMutations(HandlerExecutionContext executionContext) {
		final List<UpdateStatement> updateStatements = SqmUpdateToSqlAstConverterMultiTable.interpret(
				getSqmDeleteOrUpdateStatement(),
				generateIdTableSelect(),
				executionContext.getQueryOptions(),
				this
		);

		for ( UpdateStatement updateStatement : updateStatements ) {
			// convert each SQL AST UpdateStatement into a JdbcUpdate operation
			// 		and execute it

			final JdbcUpdate jdbcUpdate = SqlUpdateToJdbcUpdateConverter.interpret(
					updateStatement,
					executionContext.getSession(),
					executionContext.getParameterBindingContext().getQueryParameterBindings()
			);

			JdbcMutationExecutor.NO_AFTER_STATEMENT_CALL.execute(
					jdbcUpdate,
					executionContext.getQueryOptions(),
					Connection::prepareStatement,
					executionContext.getParameterBindingContext(),
					afterLoadAction -> {},
					executionContext.getSession()
			);

		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getCreationContext().getSessionFactory();
	}

	@Override
	public Callback getCallback() {
		return afterLoadAction -> {};
	}
}
