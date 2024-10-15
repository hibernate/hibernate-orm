/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.results.jdbc.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.ResultSetMappingImpl;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.type.descriptor.jdbc.NullJdbcType;

import java.util.List;

/**
 * Standard JdbcValuesMappingProducerProvider implementation
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingProducerProviderStandard implements JdbcValuesMappingProducerProvider {
	/**
	 * Singleton access
	 */
	public static final JdbcValuesMappingProducerProviderStandard INSTANCE = new JdbcValuesMappingProducerProviderStandard();

	@Override
	public JdbcValuesMappingProducer buildMappingProducer(
			SelectStatement sqlAst,
			SessionFactoryImplementor sessionFactory) {
		return new JdbcValuesMappingProducerStandard( getSelections( sqlAst ), sqlAst.getDomainResultDescriptors() );
	}

	private static List<SqlSelection> getSelections(SelectStatement selectStatement) {
		if ( selectStatement.getQueryPart() instanceof QueryGroup ) {
			final QueryGroup queryGroup = (QueryGroup) selectStatement.getQueryPart();
			for ( QueryPart queryPart : queryGroup.getQueryParts() ) {
				if ( !(queryPart.getFirstQuerySpec().getSelectClause().getSqlSelections()
						.get( 0 ).getExpressionType().getSingleJdbcMapping().getJdbcType() instanceof NullJdbcType) ) {
					return queryPart.getFirstQuerySpec().getSelectClause().getSqlSelections();
				}
			}
		}
		return selectStatement.getQuerySpec().getSelectClause().getSqlSelections();
	}

	@Override
	public ResultSetMapping buildResultSetMapping(
			String name,
			boolean isDynamic,
			SessionFactoryImplementor sessionFactory) {
		return new ResultSetMappingImpl( name, isDynamic );
	}
}
