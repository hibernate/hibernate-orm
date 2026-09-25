package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.metamodel.mapping.SqlTypedMapping;

/**
 * An expression that has SQL type information.
 */
public interface SqlTypedExpression extends Expression {
	SqlTypedMapping getSqlTypedMapping();
}
