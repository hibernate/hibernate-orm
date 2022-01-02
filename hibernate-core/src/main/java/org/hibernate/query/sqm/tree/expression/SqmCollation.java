/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;

/**
 * @author Christian Beikov
 */
public class SqmCollation extends SqmLiteral<String> {
	public SqmCollation(String value, SqmExpressable<String> inherentType, NodeBuilder nodeBuilder) {
		super(value, inherentType, nodeBuilder);
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
