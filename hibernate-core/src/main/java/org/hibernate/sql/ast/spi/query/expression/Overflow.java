package org.hibernate.sql.ast.spi.query.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;

/**
 * @author Christian Beikov
 */
public class Overflow implements Expression, SqlExpressible, SqlAstNode {
	private final Expression separatorExpression;
	private final Expression fillerExpression;
	private final boolean withCount;

	public Overflow(Expression separatorExpression, Expression fillerExpression, boolean withCount) {
		this.separatorExpression = separatorExpression;
		this.fillerExpression = fillerExpression;
		this.withCount = withCount;
	}

	public Expression getSeparatorExpression() {
		return separatorExpression;
	}

	public Expression getFillerExpression() {
		return fillerExpression;
	}

	public boolean isWithCount() {
		return withCount;
	}

	@Nullable
	@Override
	public JdbcMapping getJdbcMapping() {
		return ( (SqlExpressible) separatorExpression ).getJdbcMapping();
	}

	@Nullable
	@Override
	public JdbcMappingContainer getExpressionType() {
		return separatorExpression.getExpressionType();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitOverflow( this );
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
