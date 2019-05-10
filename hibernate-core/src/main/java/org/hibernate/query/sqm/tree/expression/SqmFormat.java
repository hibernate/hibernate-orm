/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Gavin King
 */
public class SqmFormat extends SqmLiteral<String> {
	public SqmFormat(
			String value,
			BasicValuedExpressableType<String> inherentType,
			NodeBuilder nodeBuilder) {
		super(value, inherentType, nodeBuilder);
	}

	@Override
	public <R> R accept(SemanticQueryWalker<R> walker) {
		return walker.visitFormat(this);
	}
}
