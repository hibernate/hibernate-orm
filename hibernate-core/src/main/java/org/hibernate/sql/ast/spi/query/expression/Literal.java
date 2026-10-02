package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public interface Literal extends JdbcParameterBinder, Expression {
	Object getLiteralValue();
	JdbcMapping getJdbcMapping();
}
