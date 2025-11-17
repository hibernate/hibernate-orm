/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.internal.ResultSetMappingImpl;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.type.descriptor.jdbc.NullJdbcType;

import java.util.List;

/**
 * Standard {@link JdbcValuesMappingProducerProvider} implementation
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
		if ( selectStatement.getQueryPart() instanceof QueryGroup queryGroup ) {
			for ( var queryPart : queryGroup.getQueryParts() ) {
				final var selectClause = queryPart.getFirstQuerySpec().getSelectClause();
				if ( !( selectClause.getSqlSelections().get( 0 )
						.getExpressionType().getSingleJdbcMapping().getJdbcType()
								instanceof NullJdbcType ) ) {
					return selectClause.getSqlSelections();
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
