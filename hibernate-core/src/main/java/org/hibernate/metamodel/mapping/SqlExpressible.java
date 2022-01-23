/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * Unifying contract for things that are capable of being an expression in
 * the SQL AST.
 *
 * todo (6.0) : consider adding `#toSqlExpression` returning a {@link org.hibernate.sql.ast.tree.expression.Expression}
 *
 * @author Steve Ebersole
 */
public interface SqlExpressible extends JdbcMappingContainer {
	/**
	 * Anything that is expressible at the SQL AST level
	 * would be of basic type.
	 */
	JdbcMapping getJdbcMapping();
}
