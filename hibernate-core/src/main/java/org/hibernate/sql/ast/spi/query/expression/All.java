package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * @author Loïc Lefèvre
 */
public class All implements Expression {
	/**
	 * Singleton access
	 */
	public static final All INSTANCE = new All();

	@Override
	public MappingModelExpressible getExpressionType() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitAll( this );
	}
}
