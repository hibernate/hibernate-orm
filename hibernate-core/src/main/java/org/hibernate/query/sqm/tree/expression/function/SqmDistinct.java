package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Gavin King
 */
public class SqmDistinct<T> extends AbstractSqmNode implements SqmTypedNode<T>, SqmVisitableNode {

    private final SqmExpression<T> expression;

    public SqmDistinct(SqmExpression<T> expression, NodeBuilder builder) {
        super(builder);
        this.expression = expression;
    }

    public SqmExpression<T> getExpression() {
        return expression;
    }

    @Override
    public ExpressableType<T> getExpressableType() {
        return expression.getExpressableType();
    }

    @Override
    public <X> X accept(SemanticQueryWalker<X> walker) {
        return walker.visitDistinct(this);
    }
}
