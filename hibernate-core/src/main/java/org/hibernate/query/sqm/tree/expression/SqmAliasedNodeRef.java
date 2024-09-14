/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * Models a reference to a {@link org.hibernate.query.sqm.tree.select.SqmAliasedNode}
 * used in the order-by or group-by clause by either position or alias,
 * though the reference is normalized here to a positional ref
 */
public class SqmAliasedNodeRef extends AbstractSqmExpression<Integer> {

	private final int position;
	// The navigable path is optionally set in case this is a reference to an attribute of a selection
	private final NavigablePath navigablePath;

	public SqmAliasedNodeRef(int position, SqmExpressible<Integer> intType, NodeBuilder criteriaBuilder) {
		super( intType, criteriaBuilder );
		this.position = position;
		this.navigablePath = null;
	}

	public SqmAliasedNodeRef(
			int position,
			NavigablePath navigablePath,
			SqmExpressible<Integer> type,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.position = position;
		this.navigablePath = navigablePath;
	}

	private SqmAliasedNodeRef(SqmAliasedNodeRef original) {
		super( original.getNodeType(), original.nodeBuilder() );
		this.position = original.position;
		this.navigablePath = original.navigablePath;
	}

	@Override
	public SqmAliasedNodeRef copy(SqmCopyContext context) {
		final SqmAliasedNodeRef existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmAliasedNodeRef expression = context.registerCopy( this, new SqmAliasedNodeRef( this ) );
		copyTo( expression, context );
		return expression;
	}

	public int getPosition() {
		return position;
	}

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		// we expect this to be handled specially in
		// `BaseSqmToSqlAstConverter#resolveGroupOrOrderByExpression`
		throw new UnsupportedOperationException();
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		if ( navigablePath == null ) {
			sb.append( position );
		}
		else {
			sb.append( navigablePath.getLocalName() );
		}
	}
}
