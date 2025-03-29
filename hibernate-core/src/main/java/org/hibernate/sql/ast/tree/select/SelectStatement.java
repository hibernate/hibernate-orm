/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.select;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.AbstractStatement;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SelectStatement extends AbstractStatement implements SqlAstNode, Expression, DomainResultProducer {
	private final QueryPart queryPart;
	private final List<DomainResult<?>> domainResults;

	public SelectStatement(QueryPart queryPart) {
		this( queryPart, Collections.emptyList() );
	}

	public SelectStatement(QueryPart queryPart, List<DomainResult<?>> domainResults) {
		this( null, queryPart, domainResults );
	}

	public SelectStatement(
			CteContainer cteContainer,
			QueryPart queryPart,
			List<DomainResult<?>> domainResults) {
		super( cteContainer );
		this.queryPart = queryPart;
		this.domainResults = domainResults;
	}

	public QuerySpec getQuerySpec() {
		return queryPart.getFirstQuerySpec();
	}

	public QueryPart getQueryPart() {
		return queryPart;
	}

	public List<DomainResult<?>> getDomainResultDescriptors() {
		return domainResults;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitSelectStatement( this );
	}

	@Override
	public DomainResult createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final SelectClause selectClause = queryPart.getFirstQuerySpec().getSelectClause();
		final TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState()
				.getCreationContext()
				.getMappingMetamodel()
				.getTypeConfiguration();
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		if ( selectClause.getSqlSelections().size() == 1 ) {
			final SqlSelection first = selectClause.getSqlSelections().get( 0 );
			final JdbcMapping jdbcMapping = first.getExpressionType().getSingleJdbcMapping();

			final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
					this,
					jdbcMapping.getJdbcJavaType(),
					null,
					typeConfiguration
			);

			return new BasicResult<>(
					sqlSelection.getValuesArrayPosition(),
					resultVariable,
					jdbcMapping
			);
		}
		else {
			throw new UnsupportedOperationException("Domain result for non-scalar subquery shouldn't be created");
		}
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SelectClause selectClause = queryPart.getFirstQuerySpec().getSelectClause();
		final TypeConfiguration typeConfiguration = creationState.getSqlAstCreationState()
				.getCreationContext()
				.getMappingMetamodel()
				.getTypeConfiguration();
		for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
			sqlSelection.getExpressionType().forEachJdbcType(
					(index, jdbcMapping) -> {
						creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
								this,
								jdbcMapping.getJdbcJavaType(),
								null,
								typeConfiguration
						);
					}
			);
		}
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		final SelectClause selectClause = queryPart.getFirstQuerySpec().getSelectClause();
		final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
		switch ( sqlSelections.size() ) {
			case 1:
				return sqlSelections.get( 0 ).getExpressionType();
			default:
				// todo (6.0): At some point we should create an ArrayTupleType and return that
			case 0:
				return null;
		}
	}
}
