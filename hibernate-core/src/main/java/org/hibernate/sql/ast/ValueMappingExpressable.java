/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.metamodel.model.mapping.spi.ValueMapping;

/**
 * Something that is expressable at the mapping-model type level as a type for
 * `SqlExpression` whether the SQL AST node is simple or compound.
 *
 * todo (6.0) : in that case it seems like `SqlExpression` should be a `ValueMappingExpressable`. `SqlTuple` is
 * 		a special case for compound expressions - the `ValueMapping` would be the "composite type"
 *
 * todo (6.0) : does adding a SqlAstExpressionProducer make sense?
 *
 * @author Steve Ebersole
 */
public interface ValueMappingExpressable {
	/**
	 * The ValueMapping for this expressable
	 */
	ValueMapping getExpressableValueMapping();
}
