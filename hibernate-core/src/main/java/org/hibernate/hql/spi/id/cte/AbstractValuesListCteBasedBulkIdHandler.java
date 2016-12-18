/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.JDBCException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AbstractRestrictableStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.spi.id.AbstractTableBasedBulkIdHandler;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public abstract class AbstractValuesListCteBasedBulkIdHandler extends
		AbstractTableBasedBulkIdHandler {

	private final Queryable targetedPersister;

	private final String idSelect;
	private final List<ParameterSpecification> idSelectParameterSpecifications;

	private final String catalog;
	private final String schema;

	private final JdbcEnvironment jdbcEnvironment;

	public AbstractValuesListCteBasedBulkIdHandler(
			SessionFactoryImplementor sessionFactory, HqlSqlWalker walker,
			String catalog, String schema) {
		super(sessionFactory, walker);
		Dialect dialect = factory().getDialect();
		if ( !dialect.supportsNonQueryInCTE() || !dialect.supportsValuesList() ) {
			throw new UnsupportedOperationException(
					"The " + getClass().getSimpleName() +
					" can only be used with Dialects that support VALUES lists and CTE that can take UPDATE or DELETE statements as well!"
			);
		}

		this.jdbcEnvironment = sessionFactory.getServiceRegistry().getService( JdbcServices.class ).getJdbcEnvironment();
		this.catalog = catalog;
		this.schema = schema;

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

	protected JDBCException convert(
			SQLException e,
			String message,
			String sql) {
		throw factory().getSQLExceptionHelper().convert( e, message, sql );
	}

	protected String determineIdTableName(Queryable persister) {
		return jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				new QualifiedTableName(
						Identifier.toIdentifier( catalog ),
						Identifier.toIdentifier( schema ),
						Identifier.toIdentifier( "HT_" + persister.getTableName() )
				),
				jdbcEnvironment.getDialect()
		);
	}

	protected String generateIdSubselect(Queryable persister) {
		return new StringBuilder()
			.append( "select " )
			.append( StringHelper.join( ", ", persister.getIdentifierColumnNames() ) )
			.append( " from " )
			.append( determineIdTableName( persister ) )
			.toString();
	}

	protected ValuesListCteBuilder prepareCteStatement(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {

		try {
			try (PreparedStatement ps = session.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( idSelect, false )) {
				int sum = 1;
				for ( ParameterSpecification parameterSpecification : idSelectParameterSpecifications ) {
					sum += parameterSpecification.bind( ps, queryParameters, session, sum );
				}
				List<Object[]> ids = new ArrayList<>();

				ResultSet rs = session
						.getJdbcCoordinator()
						.getResultSetReturn()
						.extract( ps );
				while ( rs.next() ) {
					Object[] result = new Object[targetedPersister.getIdentifierColumnNames().length];
					for ( String columnName : targetedPersister.getIdentifierColumnNames() ) {
						Object column = rs.getObject( columnName );
						result[rs.findColumn( columnName ) - 1] = column;
					}
					ids.add( result );
				}

				if ( ids.isEmpty() ) {
					return null;
				}

				return new ValuesListCteBuilder(
						determineIdTableName( targetedPersister ), targetedPersister.getIdentifierColumnNames(), ids
				);
			}
		}
		catch ( SQLException e ) {
			throw convert( e, "could not insert/select ids for bulk operation", idSelect );
		}
	}
}
