/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.AbstractIdsBulkIdHandler;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.Queryable;

/**
 * Defines how identifier values are selected from the updatable/deletable tables.
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public abstract class AbstractCteValuesListBulkIdHandler extends
		AbstractIdsBulkIdHandler {

	private final String catalog;
	private final String schema;

	private final JdbcEnvironment jdbcEnvironment;

	public AbstractCteValuesListBulkIdHandler(
			SessionFactoryImplementor sessionFactory, HqlSqlWalker walker,
			String catalog, String schema) {
		super( sessionFactory, walker );
		Dialect dialect = sessionFactory.getServiceRegistry().getService( JdbcServices.class ).getDialect();
		if ( !dialect.supportsNonQueryWithCTE() ) {
			throw new UnsupportedOperationException(
					"The " + getClass().getSimpleName() +
							" can only be used with Dialects that support CTE that can take UPDATE or DELETE statements as well!"
			);
		}
		if ( !dialect.supportsValuesList() ) {
			throw new UnsupportedOperationException(
					"The " + getClass().getSimpleName() +
							" can only be used with Dialects that support VALUES lists!"
			);
		}
		if ( !dialect.supportsRowValueConstructorSyntaxInInList() ) {
			throw new UnsupportedOperationException(
					"The " + getClass().getSimpleName() +
							" can only be used with Dialects that support IN clause row-value expressions (for composite identifiers)!"
			);
		}

		this.jdbcEnvironment = sessionFactory.getServiceRegistry().getService(
				JdbcServices.class ).getJdbcEnvironment();
		this.catalog = catalog;
		this.schema = schema;
	}

	protected String determineIdTableName(Queryable persister) {

		String qualifiedTableName = jdbcEnvironment.getIdentifierHelper().applyGlobalQuoting(
				"HT_" + StringHelper.unquote( persister.getTableName(), jdbcEnvironment.getDialect() )
		).render();

		return jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				new QualifiedTableName(
						Identifier.toIdentifier( catalog ),
						Identifier.toIdentifier( schema ),
						Identifier.toIdentifier( qualifiedTableName )
				),
				jdbcEnvironment.getDialect()
		);
	}

	protected String generateIdSubselect(Queryable persister) {
		return new StringBuilder()
				.append( "select " )
				.append( String.join(
						", ",
						(CharSequence[]) persister.getIdentifierColumnNames()
				) )
				.append( " from " )
				.append( determineIdTableName( persister ) )
				.toString();
	}

	protected CteValuesListBuilder prepareCteStatement(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {

		return new CteValuesListBuilder(
				determineIdTableName( getTargetedQueryable() ),
				getTargetedQueryable().getIdentifierColumnNames(),
				selectIds( session, queryParameters )
		);
	}
}
