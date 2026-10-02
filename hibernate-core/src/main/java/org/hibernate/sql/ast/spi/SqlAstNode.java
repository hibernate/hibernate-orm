package org.hibernate.sql.ast.spi;


/**
 * @author Steve Ebersole
 */
public interface SqlAstNode {
	void accept(SqlAstWalker sqlTreeWalker);
}
