/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Christian Beikov
 */
public class SqmCollation extends SqmLiteral<String> {
	public SqmCollation(String value, SqmExpressible<String> inherentType, NodeBuilder nodeBuilder) {
		super(value, inherentType, nodeBuilder);
	}

	@Override
	public SqmCollation copy(SqmCopyContext context) {
		final SqmCollation existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCollation expression = context.registerCopy(
				this,
				new SqmCollation( getLiteralValue(), getNodeType(), nodeBuilder() )
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <R> R accept(SemanticQueryWalker<R> walker) {
		return walker.visitCollation( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( getLiteralValue() );
	}
}
