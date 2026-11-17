package org.hibernate.query.sqm.tree.spi.expression;

import java.util.List;
import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.sql.ast.spi.query.expression.JsonPathPassingClause;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base class for expressions that contain a json path. Maintains a map of expressions for identifiers.
 *
 * @since 7.0
 */
@Incubating(since = "6.2")
public abstract class AbstractSqmJsonPathExpression<T> extends SelfRenderingSqmFunction<T> {

	private final SqmJsonPathPassingClause passingClause;

	public AbstractSqmJsonPathExpression(
			@Nonnull SqmFunctionDescriptor descriptor,
			@Nonnull FunctionRenderer renderer,
			@Nonnull List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nonnull FunctionReturnTypeResolver returnTypeResolver,
			@Nonnull NodeBuilder nodeBuilder,
			@Nonnull String name) {
		this(
				descriptor,
				renderer,
				arguments,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name,
				null
		);
	}

	protected AbstractSqmJsonPathExpression(
			@Nonnull SqmFunctionDescriptor descriptor,
			@Nonnull FunctionRenderer renderer,
			@Nonnull List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nonnull FunctionReturnTypeResolver returnTypeResolver,
			@Nonnull NodeBuilder nodeBuilder,
			@Nonnull String name,
			@Nullable Map<String, SqmExpression<?>> passingExpressions) {
		super(
				descriptor,
				renderer,
				arguments,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name
		);
		this.passingClause = new SqmJsonPathPassingClause( passingExpressions );
	}

	@Nonnull
	public Map<String, SqmExpression<?>> getPassingExpressions() {
		return passingClause.getExpressions();
	}

	protected void addPassingExpression(@Nonnull String identifier, @Nonnull SqmExpression<?> expression) {
		passingClause.add( identifier, expression );
	}

	protected @Nullable Map<String, SqmExpression<?>> copyPassingExpressions(@Nonnull SqmCopyContext context) {
		return passingClause.copyExpressions( context );
	}

	protected @Nullable JsonPathPassingClause createJsonPathPassingClause(@Nonnull SqmToSqlAstConverter walker) {
		return passingClause.convertToSqlAst( walker );
	}

	protected void appendPassingExpressionHqlString(@Nonnull StringBuilder sb, @Nonnull SqmRenderContext context) {
		passingClause.appendHqlString( sb, context );
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return super.equals( other )
			&& other instanceof AbstractSqmJsonPathExpression<?> that
			&& passingClause.equals( that.passingClause );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + passingClause.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object other) {
		return super.isCompatible( other )
			&& other instanceof AbstractSqmJsonPathExpression<?> that
			&& passingClause.isCompatible( that.passingClause );
	}

	@Override
	public int cacheHashCode() {
		int result = super.cacheHashCode();
		result = 31 * result + passingClause.cacheHashCode();
		return result;
	}
}
