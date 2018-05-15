/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * Specialization of a literal expression which is a reference to user
 * code value.  Two flavors:
 *
 * 		* Reference to an enum value
 * 		* Reference to a constant field
 *
 * @author Steve Ebersole
 */
public interface SqmConstantReference<T> extends SqmLiteral {
	@Override
	BasicValuedExpressableType getExpressableType();

	@Override
	BasicValuedExpressableType getInferableType();
}
