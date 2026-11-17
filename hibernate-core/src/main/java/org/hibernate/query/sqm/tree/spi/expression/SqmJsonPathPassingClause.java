package org.hibernate.query.sqm.tree.spi.expression;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nullable;
import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JsonPathPassingClause;

/**
 * Bindings for the named variables in a JSON path.
 */
final class SqmJsonPathPassingClause implements SqmCacheable, Serializable {
	private @Nullable Map<String, SqmExpression<?>> expressions;

	SqmJsonPathPassingClause(@Nullable Map<String, SqmExpression<?>> expressions) {
		this.expressions = expressions;
	}

	Map<String, SqmExpression<?>> getExpressions() {
		return expressions == null ? Collections.emptyMap() : Collections.unmodifiableMap( expressions );
	}

	void add(String identifier, SqmExpression<?> expression) {
		if ( expressions == null ) {
			expressions = new HashMap<>();
		}
		expressions.put( identifier, expression );
	}

	@Nullable Map<String, SqmExpression<?>> copyExpressions(SqmCopyContext context) {
		if ( expressions == null ) {
			return null;
		}
		final var copy = new HashMap<String, SqmExpression<?>>( expressions.size() );
		for ( var entry : expressions.entrySet() ) {
			copy.put( entry.getKey(), entry.getValue().copy( context ) );
		}
		return copy;
	}

	void copyTo(SqmJsonPathPassingClause target, SqmCopyContext context) {
		target.expressions = copyExpressions( context );
	}

	@Nullable JsonPathPassingClause convertToSqlAst(SqmToSqlAstConverter walker) {
		if ( expressions == null || expressions.isEmpty() ) {
			return null;
		}
		final var converted = new HashMap<String, Expression>( expressions.size() );
		for ( var entry : expressions.entrySet() ) {
			converted.put( entry.getKey(), (Expression) entry.getValue().accept( walker ) );
		}
		return new JsonPathPassingClause( converted );
	}

	void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( expressions != null ) {
			String separator = " passing ";
			for ( var entry : expressions.entrySet() ) {
				hql.append( separator );
				entry.getValue().appendHqlString( hql, context );
				hql.append( " as " );
				QuotingHelper.appendQuotedIdentifier( hql, entry.getKey() );
				separator = ", ";
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return other instanceof SqmJsonPathPassingClause that
			&& Objects.equals( expressions, that.expressions );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( expressions );
	}

	@Override
	public boolean isCompatible(@Nullable Object other) {
		return other instanceof SqmJsonPathPassingClause that
			&& SqmCacheable.areCompatible( expressions, that.expressions );
	}

	@Override
	public int cacheHashCode() {
		return SqmCacheable.cacheHashCode( expressions );
	}
}
