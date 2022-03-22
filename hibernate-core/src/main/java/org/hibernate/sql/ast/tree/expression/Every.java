/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Gavin King
 */
public class Every implements Expression, DomainResultProducer {

	private final QueryPart subquery;
	private final MappingModelExpressible<?> type;

	public Every(QueryPart subquery, MappingModelExpressible<?> type) {
		this.subquery = subquery;
		this.type = type;
	}

	public QueryPart getSubquery() {
		return subquery;
	}

	@Override
	public MappingModelExpressible getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitEvery( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final JavaType javaType = type.getJdbcMappings().get( 0 ).getJavaTypeDescriptor();
		return new BasicResult(
				creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
						this,
						javaType,
						null,
						creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
				).getValuesArrayPosition(),
				resultVariable,
				javaType
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		sqlExpressionResolver.resolveSqlSelection(
				this,
				type.getJdbcMappings().get( 0 ).getJavaTypeDescriptor(),
				null,
				sqlAstCreationState.getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}
}
