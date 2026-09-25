package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmUnaryOperation<T> extends AbstractSqmExpression<T> implements SqmSelectableNode<T> {
	private final UnaryArithmeticOperator operation;
	private final SqmExpression<T> operand;

	public SqmUnaryOperation(
			@Nonnull UnaryArithmeticOperator operation,
			@Nonnull SqmExpression<T> operand,
			@Nonnull NodeBuilder nodeBuilder) {
		//noinspection unchecked
		this(
				operation,
				operand,
				(SqmBindableType<T>) // TODO: this cast is unsound
						nodeBuilder.getTypeConfiguration()
								.resolveArithmeticType( castNonNull( operand.getExpressible() ) ),
				nodeBuilder
		);
	}

	public SqmUnaryOperation(
			@Nonnull UnaryArithmeticOperator operation,
			@Nonnull SqmExpression<T> operand,
			@Nullable SqmBindableType<T> inherentType,
			@Nonnull NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.operation = operation;
		this.operand = operand;
	}

	@Nonnull
	@Override
	public SqmUnaryOperation<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmUnaryOperation<T> expression = context.registerCopy(
				this,
				new SqmUnaryOperation<>(
						operation,
						operand.copy( context ),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Nonnull
	public SqmExpression<T> getOperand() {
		return operand;
	}

	@Nonnull
	public UnaryArithmeticOperator getOperation() {
		return operation;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitUnaryOperationExpression( this );
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return operation.getOperatorChar() + operand.asLoggableText();
	}
	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( operation.getOperatorChar() );
		operand.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmUnaryOperation<?> that
			&& operation == that.getOperation()
			&& operand.equals( that.getOperand() );
	}

	@Override
	public int hashCode() {
		int result = operation.hashCode();
		result = 31 * result + operand.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmUnaryOperation<?> that
				&& operation == that.getOperation()
				&& operand.isCompatible( that.getOperand() );
	}

	@Override
	public int cacheHashCode() {
		int result = operation.hashCode();
		result = 31 * result + operand.cacheHashCode();
		return result;
	}
}
