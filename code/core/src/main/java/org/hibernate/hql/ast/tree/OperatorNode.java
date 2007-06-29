package org.hibernate.hql.ast.tree;

import org.hibernate.type.Type;
import antlr.SemanticException;

/**
 * Contract for nodes representing operators (logic or arithmetic).
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface OperatorNode {
	/**
	 * Called by the tree walker during hql-sql semantic analysis
	 * after the operator sub-tree is completely built.
	 */
	public abstract void initialize() throws SemanticException;

	/**
	 * Retrieves the data type for the overall operator expression.
	 *
	 * @return The expression's data type.
	 */
	public Type getDataType();
}
