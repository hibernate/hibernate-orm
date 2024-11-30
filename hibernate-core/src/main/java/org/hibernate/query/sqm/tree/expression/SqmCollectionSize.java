/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * Represents the {@code SIZE()} function.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public class SqmCollectionSize extends AbstractSqmExpression<Integer> {
	private final SqmPath<?> pluralPath;

	public SqmCollectionSize(SqmPath<?> pluralPath, NodeBuilder nodeBuilder) {
		this( pluralPath, nodeBuilder.getIntegerType(), nodeBuilder );
	}

	public SqmCollectionSize(SqmPath<?> pluralPath, SqmExpressible<Integer> sizeType, NodeBuilder nodeBuilder) {
		super( sizeType, nodeBuilder );
		this.pluralPath = pluralPath;
	}

	@Override
	public SqmCollectionSize copy(SqmCopyContext context) {
		final SqmCollectionSize existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCollectionSize expression = context.registerCopy(
				this,
				new SqmCollectionSize(
						pluralPath.copy( context ),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmPath<?> getPluralPath() {
		return pluralPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeSizeFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "SIZE(" + pluralPath.asLoggableText() + ")";
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "size(" );
		pluralPath.appendHqlString( sb );
		sb.append( ')' );
	}

}
