/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class Collate implements Expression, SqlExpressable, SqlAstNode, DomainResultProducer {

	private final Expression expression;
	private final String collation;

	public Collate(Expression expression, String collation) {
		this.expression = expression;
		this.collation = collation;
	}

	public Expression getExpression() {
		return expression;
	}

	public String getCollation() {
		return collation;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		if ( expression instanceof SqlExpressable ) {
			return ( (SqlExpressable) expression ).getJdbcMapping();
		}

		if ( getExpressionType() instanceof SqlExpressable ) {
			return ( (SqlExpressable) getExpressionType() ).getJdbcMapping();
		}

		if ( getExpressionType() != null ) {
			final JdbcMappingContainer mappingContainer = getExpressionType();
			assert mappingContainer.getJdbcTypeCount() == 1;
			return mappingContainer.getJdbcMappings().get( 0 );
		}

		return null;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expression.getExpressionType();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCollate( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final JavaType javaTypeDescriptor = expression.getExpressionType().getJdbcMappings().get( 0 ).getJavaTypeDescriptor();
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

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		sqlExpressionResolver.resolveSqlSelection(
				this,
				expression.getExpressionType().getJdbcMappings().get( 0 ).getJavaTypeDescriptor(),
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
