/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.FrameExclusion;
import org.hibernate.query.sqm.FrameKind;
import org.hibernate.query.sqm.FrameMode;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

/**
 * @author Christian Beikov
 */
public class SqmOver<T> extends AbstractSqmExpression<T> {

	private final SqmExpression<T> expression;
	private final List<SqmExpression<?>> partitions;
	private final List<SqmSortSpecification> orderList;
	private final FrameMode mode;
	private final FrameKind startKind;
	private final SqmExpression<?> startExpression;
	private final FrameKind endKind;
	private final SqmExpression<?> endExpression;
	private final FrameExclusion exclusion;

	public SqmOver(
			SqmExpression<T> expression,
			List<SqmExpression<?>> partitions,
			List<SqmSortSpecification> orderList,
			FrameMode mode,
			FrameKind startKind,
			SqmExpression<?> startExpression,
			FrameKind endKind,
			SqmExpression<?> endExpression,
			FrameExclusion exclusion) {
		super( expression.getNodeType(), expression.nodeBuilder() );
		this.expression = expression;
		this.partitions = partitions;
		this.orderList = orderList;
		this.mode = mode;
		this.startKind = startKind;
		this.startExpression = startExpression;
		this.endKind = endKind;
		this.endExpression = endExpression;
		this.exclusion = exclusion;
	}

	@Override
	public SqmOver<T> copy(SqmCopyContext context) {
		final SqmOver<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmExpression<?>> partitions = new ArrayList<>(this.partitions.size());
		for ( SqmExpression<?> partition : this.partitions ) {
			partitions.add( partition.copy( context ) );
		}
		final List<SqmSortSpecification> orderList = new ArrayList<>(this.orderList.size());
		for ( SqmSortSpecification sortSpecification : this.orderList ) {
			orderList.add( sortSpecification.copy( context ) );
		}
		final SqmOver<T> over = context.registerCopy(
				this,
				new SqmOver<>(
						expression.copy( context ),
						partitions,
						orderList,
						mode,
						startKind,
						startExpression == null ? null : startExpression.copy( context ),
						endKind,
						endExpression == null ? null : endExpression.copy( context ),
						exclusion
				)
		);
		copyTo( over, context );
		return over;
	}

	public SqmExpression<T> getExpression() {
		return expression;
	}

	public List<SqmExpression<?>> getPartitions() {
		return partitions;
	}

	public List<SqmSortSpecification> getOrderList() {
		return orderList;
	}

	public SqmExpression<?> getStartExpression() {
		return startExpression;
	}

	public SqmExpression<?> getEndExpression() {
		return endExpression;
	}

	public FrameMode getMode() {
		return mode;
	}

	public FrameKind getStartKind() {
		return startKind;
	}

	public FrameKind getEndKind() {
		return endKind;
	}

	public FrameExclusion getExclusion() {
		return exclusion;
	}

	@Override
	public SqmExpressible<T> getNodeType() {
		return expression.getNodeType();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitOver( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		expression.appendHqlString( sb );
		sb.append( " over (" );
		boolean needsWhitespace = false;
		if (!partitions.isEmpty()) {
			needsWhitespace = true;
			sb.append( "partition by " );
			partitions.get( 0 ).appendHqlString( sb );
			for ( int i = 1; i < partitions.size(); i++ ) {
				sb.append( ',' );
				partitions.get( i ).appendHqlString( sb );
			}
		}
		if (!orderList.isEmpty()) {
			if ( needsWhitespace ) {
				sb.append( ' ' );
			}
			needsWhitespace = true;
			sb.append( "order by " );
			orderList.get( 0 ).appendHqlString( sb );
			for ( int i = 1; i < orderList.size(); i++ ) {
				sb.append( ',' );
				orderList.get( i ).appendHqlString( sb );
			}
		}
		if ( mode == FrameMode.ROWS && startKind == FrameKind.UNBOUNDED_PRECEDING && endKind == FrameKind.CURRENT_ROW && exclusion == FrameExclusion.NO_OTHERS ) {
			// This is the default, so we don't need to render anything
		}
		else {
			if ( needsWhitespace ) {
				sb.append( ' ' );
			}
			switch ( mode ) {
				case GROUPS:
					sb.append( "groups " );
					break;
				case RANGE:
					sb.append( "range " );
					break;
				case ROWS:
					sb.append( "rows " );
					break;
			}
			if ( endKind == FrameKind.CURRENT_ROW ) {
				renderFrameKind( sb, startKind, startExpression );
			}
			else {
				sb.append( "between " );
				renderFrameKind( sb, startKind, startExpression );
				sb.append( " and " );
				renderFrameKind( sb, endKind, endExpression );
			}
			switch ( exclusion ) {
				case TIES:
					sb.append( " exclude ties" );
					break;
				case CURRENT_ROW:
					sb.append( " exclude current row" );
					break;
				case GROUP:
					sb.append( " exclude group" );
					break;
			}
		}
		sb.append( ')' );
	}

	private static void renderFrameKind(StringBuilder sb, FrameKind kind, SqmExpression<?> expression) {
		switch ( kind ) {
			case CURRENT_ROW:
				sb.append( "current row" );
				break;
			case UNBOUNDED_PRECEDING:
				sb.append( "unbounded preceding" );
				break;
			case UNBOUNDED_FOLLOWING:
				sb.append( "unbounded following" );
				break;
			case OFFSET_PRECEDING:
				expression.appendHqlString( sb );
				sb.append( " preceding" );
				break;
			case OFFSET_FOLLOWING:
				expression.appendHqlString( sb );
				sb.append( " following" );
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported frame kind: " + kind );
		}
	}
}
