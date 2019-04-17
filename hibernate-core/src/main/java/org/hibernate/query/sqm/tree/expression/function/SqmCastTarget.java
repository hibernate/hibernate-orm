package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Gavin King
 */
public class SqmCastTarget implements SqmExpression {
	private AllowableFunctionReturnType type;

	public SqmCastTarget(AllowableFunctionReturnType type) {
		this.type = type;
	}

	@Override
	public AllowableFunctionReturnType getExpressableType() {
		return type;
	}

	@Override
	public void applyInferableType(ExpressableType<?> type) {}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCastTarget(this);
	}
}
