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
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Assertion that the SQL state of a "mapping model expressable" is null
 */
public class NullnessLiteral implements Expression, DomainResultProducer<Boolean> {
	private final MappingModelExpressable<?> valueMapping;

	public NullnessLiteral(MappingModelExpressable<?> valueMapping) {
		this.valueMapping = valueMapping;
	}

	@Override
	public MappingModelExpressable<?> getExpressionType() {
		return valueMapping;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitNullnessLiteral( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final JavaTypeDescriptor javaTypeDescriptor = valueMapping.getJdbcMappings().get( 0 ).getJavaTypeDescriptor();
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
