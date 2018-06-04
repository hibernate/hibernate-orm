/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.JDBCException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AbstractRestrictableStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;

/**
 * Base class for all strategies that select the ids to be updated/deleted prior to executing the update/delete operation.
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractIdsBulkIdHandler
		extends AbstractTableBasedBulkIdHandler {

	private final Queryable targetedPersister;

	private final String idSelect;
	private final List<ParameterSpecification> idSelectParameterSpecifications;

	public AbstractIdsBulkIdHandler(
			SessionFactoryImplementor sessionFactory, HqlSqlWalker walker) {
		super(sessionFactory, walker);

		final AbstractRestrictableStatement statement = (AbstractRestrictableStatement) walker.getAST();
		final FromElement fromElement = statement.getFromClause().getFromElement();

		this.targetedPersister = fromElement.getQueryable();

		final ProcessedWhereClause processedWhereClause = processWhereClause( statement.getWhereClause() );
		this.idSelectParameterSpecifications = processedWhereClause.getIdSelectParameterSpecifications();

		final String bulkTargetAlias = fromElement.getTableAlias();

		this.idSelect = generateIdSelect( bulkTargetAlias, processedWhereClause ).toStatementString();
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	protected Dialect dialect() {
		return factory().getServiceRegistry().getService( JdbcServices.class ).getDialect();
	}

	protected JDBCException convert(
			SQLException e,
			String message,
			String sql) {
		throw factory().getServiceRegistry().getService( JdbcServices.class ).getSqlExceptionHelper().convert( e, message, sql );
	}

	protected List<Object[]> selectIds(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {
		List<Object[]> ids = new ArrayList<>();
		try {
			try (PreparedStatement ps = session.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( idSelect, false )) {
				int position = 1;
				for ( ParameterSpecification parameterSpecification : idSelectParameterSpecifications ) {
					position += parameterSpecification.bind( ps, queryParameters, session, position );
				}

				Dialect dialect = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getDialect();

				ResultSet rs = session
						.getJdbcCoordinator()
						.getResultSetReturn()
						.extract( ps );
				while ( rs.next() ) {
					Object[] result = new Object[targetedPersister.getIdentifierColumnNames().length];
					for ( String columnName : targetedPersister.getIdentifierColumnNames() ) {
						int columnIndex = rs.findColumn( StringHelper.unquote( columnName, dialect ) );
						Object column = rs.getObject(columnIndex);
						result[columnIndex - 1] = column;
					}
					ids.add( result );
				}
			}
		}
		catch ( SQLException e ) {
			throw convert( e, "could not select ids for bulk operation", idSelect );
		}

		return ids;
	}
}
