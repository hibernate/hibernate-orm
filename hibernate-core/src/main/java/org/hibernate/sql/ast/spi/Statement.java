package org.hibernate.sql.ast.spi;


/**
 * Base contract for any statement
 *
 * @author Steve Ebersole
 */
public interface Statement extends SqlAstNode {
	/**
	 * Visitation
	 */
	void accept(SqlAstWalker walker);

	/**
	 * Whether this statement is a selection and will return results.
	 */
	default boolean isSelection() {
		return false;
	}
}
