/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralNull<T> extends SqmLiteral<T> {
	public SqmLiteralNull(NodeBuilder nodeBuilder) {
		//noinspection unchecked
		this( NULL_TYPE, nodeBuilder );
	}

	public SqmLiteralNull(
			SqmExpressable<T> expressableType,
			NodeBuilder nodeBuilder) {
		super( null, expressableType, nodeBuilder );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitLiteral( this );
	}

	@Override
	public String asLoggableText() {
		return "<literal-null>";
	}

	private static SqmExpressable NULL_TYPE = () -> null;
}
