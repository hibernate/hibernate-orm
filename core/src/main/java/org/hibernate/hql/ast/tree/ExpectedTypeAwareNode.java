package org.hibernate.hql.ast.tree;

import org.hibernate.type.Type;

/**
 * Interface for nodes which wish to be made aware of any determined "expected
 * type" based on the context within they appear in the query.
 *
 * @author Steve Ebersole
 */
public interface ExpectedTypeAwareNode {
	public void setExpectedType(Type expectedType);
	public Type getExpectedType();
}
