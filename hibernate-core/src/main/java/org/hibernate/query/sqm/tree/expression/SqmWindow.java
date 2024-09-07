/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaWindow;
import org.hibernate.query.criteria.JpaWindowFrame;
import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.common.FrameMode;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;

import static org.hibernate.query.common.FrameExclusion.NO_OTHERS;
import static org.hibernate.query.common.FrameKind.CURRENT_ROW;
import static org.hibernate.query.common.FrameKind.UNBOUNDED_PRECEDING;
import static org.hibernate.query.common.FrameMode.GROUPS;
import static org.hibernate.query.common.FrameMode.RANGE;
import static org.hibernate.query.common.FrameMode.ROWS;

/**
 * @author Marco Belladelli
 */
@Incubating
public class SqmWindow extends AbstractSqmNode implements JpaWindow, SqmVisitableNode {
	private final List<SqmExpression<?>> partitions;
	private final List<SqmSortSpecification> orderList;
	private FrameMode mode;
	private FrameKind startKind;
	private SqmExpression<?> startExpression;
	private FrameKind endKind;
	private SqmExpression<?> endExpression;
	private FrameExclusion exclusion;

	public SqmWindow(NodeBuilder nodeBuilder) {
		this(
				nodeBuilder,
				new ArrayList<>(),
				new ArrayList<>(),
				RANGE,
				UNBOUNDED_PRECEDING,
				null,
				CURRENT_ROW,
				null,
				NO_OTHERS
		);
	}

	public SqmWindow(
			NodeBuilder nodeBuilder,
			List<SqmExpression<?>> partitions,
			List<SqmSortSpecification> orderList,
			FrameMode mode,
			FrameKind startKind,
			SqmExpression<?> startExpression,
			FrameKind endKind,
			SqmExpression<?> endExpression,
			FrameExclusion exclusion) {
		super( nodeBuilder );
		this.partitions = partitions;
		this.orderList = orderList;
		this.mode = mode;
		this.startKind = startKind;
		this.startExpression = startExpression;
		this.endKind = endKind;
		this.endExpression = endExpression;
		this.exclusion = exclusion;
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
	public JpaWindow frameRows(JpaWindowFrame startFrame, JpaWindowFrame endFrame) {
		return this.setFrames( ROWS, startFrame, endFrame );
	}

	@Override
	public JpaWindow frameRange(JpaWindowFrame startFrame, JpaWindowFrame endFrame) {
		return this.setFrames( RANGE, startFrame, endFrame );

	}

	@Override
	public JpaWindow frameGroups(JpaWindowFrame startFrame, JpaWindowFrame endFrame) {
		return this.setFrames( GROUPS, startFrame, endFrame );

	}

	private SqmWindow setFrames(FrameMode frameMode, JpaWindowFrame startFrame, JpaWindowFrame endFrame) {
		this.mode = frameMode;
		if ( startFrame != null ) {
			this.startKind = startFrame.getKind();
			this.startExpression = (SqmExpression<?>) startFrame.getExpression();
		}
		if ( endFrame != null ) {
			this.endKind = endFrame.getKind();
			this.endExpression = (SqmExpression<?>) endFrame.getExpression();
		}
		return this;
	}

	@Override
	public JpaWindow frameExclude(FrameExclusion frameExclusion) {
		this.exclusion = frameExclusion;
		return this;
	}

	@Override
	public JpaWindow partitionBy(Expression<?>... expressions) {
		for ( Expression<?> expression : expressions ) {
			this.partitions.add( (SqmExpression<?>) expression );
		}
		return this;
	}

	@Override
	public JpaWindow orderBy(Order... orders) {
		for ( Order order : orders ) {
			this.orderList.add( (SqmSortSpecification) order );
		}
		return this;
	}

	public SqmWindow copy(SqmCopyContext context) {
		final SqmWindow existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmExpression<?>> partitionsCopy = new ArrayList<>( partitions.size() );
		for ( SqmExpression<?> partition : partitions ) {
			partitionsCopy.add( partition.copy( context ) );
		}
		final List<SqmSortSpecification> orderListCopy = new ArrayList<>( orderList.size() );
		for ( SqmSortSpecification sortSpecification : orderList ) {
			orderListCopy.add( sortSpecification.copy( context ) );
		}
		return context.registerCopy(
				this,
				new SqmWindow(
						nodeBuilder(),
						partitionsCopy,
						orderListCopy,
						mode,
						startKind,
						startExpression == null ? null : startExpression.copy( context ),
						endKind,
						endExpression == null ? null : endExpression.copy( context ),
						exclusion
				)
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitWindow( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		boolean needsWhitespace = false;
		if ( !this.partitions.isEmpty() ) {
			needsWhitespace = true;
			sb.append( "partition by " );
			this.partitions.get( 0 ).appendHqlString( sb );
			for ( int i = 1; i < this.partitions.size(); i++ ) {
				sb.append( ',' );
				this.partitions.get( i ).appendHqlString( sb );
			}
		}
		if ( !orderList.isEmpty() ) {
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
		if ( mode == RANGE && startKind == UNBOUNDED_PRECEDING && endKind == CURRENT_ROW && exclusion == NO_OTHERS ) {
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
			if ( endKind == CURRENT_ROW ) {
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
