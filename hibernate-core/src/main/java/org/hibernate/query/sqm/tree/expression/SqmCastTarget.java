/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;


/**
 * @author Gavin King
 */
public class SqmCastTarget<T> extends AbstractSqmNode implements SqmTypedNode<T> {
	private final ReturnableType<T> type;
	private final Long length;
	private final Integer precision;
	private final Integer scale;

	public Long getLength() {
		return length;
	}

	public Integer getPrecision() {
		return precision;
	}

	public Integer getScale() {
		return scale;
	}

	public SqmCastTarget(
			ReturnableType<T> type,
			NodeBuilder nodeBuilder) {
		this( type, null, nodeBuilder );
	}

	public SqmCastTarget(
			ReturnableType<T> type,
			Long length,
			NodeBuilder nodeBuilder) {
		this( type, length, null, null, nodeBuilder );
	}

	public SqmCastTarget(
			ReturnableType<T> type,
			Integer precision,
			Integer scale,
			NodeBuilder nodeBuilder) {
		this( type, null, precision, scale, nodeBuilder );
	}

	public SqmCastTarget(
			ReturnableType<T> type,
			Long length,
			Integer precision,
			Integer scale,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.type = type;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
	}

	@Override
	public SqmCastTarget<T> copy(SqmCopyContext context) {
		return this;
	}

	public ReturnableType<T> getType() {
		return type;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCastTarget(this);
	}

	@Override
	public SqmExpressible<T> getNodeType() {
		return type;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( type.getTypeName() );
		if ( length != null ) {
			sb.append( '(' );
			sb.append( length );
			sb.append( ')' );
		}
		else if ( precision != null ) {
			sb.append( '(' );
			sb.append( precision );
			if ( scale != null ) {
				sb.append( ", " );
				sb.append( scale );
			}
			sb.append( ')' );
		}
	}
}
