/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Gavin King
 */
public class Every implements Expression, DomainResultProducer {

	private final QueryPart subquery;
	private final MappingModelExpressable<?> type;

	public Every(QueryPart subquery, MappingModelExpressable<?> type) {
		this.subquery = subquery;
		this.type = type;
	}

	public QueryPart getSubquery() {
		return subquery;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
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
		final JavaTypeDescriptor javaTypeDescriptor = type.getJdbcMappings().get( 0 ).getJavaTypeDescriptor();
		return new BasicResult(
				creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
						this,
						javaTypeDescriptor,
						creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
				).getValuesArrayPosition(),
				resultVariable,
				javaTypeDescriptor
		);
	}
}
