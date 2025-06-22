/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import org.hibernate.query.sqm.tree.SqmRenderContext;
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
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		boolean needsWhitespace = false;
		if ( !this.partitions.isEmpty() ) {
			needsWhitespace = true;
			hql.append( "partition by " );
			this.partitions.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < this.partitions.size(); i++ ) {
				hql.append( ',' );
				this.partitions.get( i ).appendHqlString( hql, context );
			}
		}
		if ( !orderList.isEmpty() ) {
			if ( needsWhitespace ) {
				hql.append( ' ' );
			}
			needsWhitespace = true;
			hql.append( "order by " );
			orderList.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < orderList.size(); i++ ) {
				hql.append( ',' );
				orderList.get( i ).appendHqlString( hql, context );
			}
		}
		if ( mode == RANGE && startKind == UNBOUNDED_PRECEDING && endKind == CURRENT_ROW && exclusion == NO_OTHERS ) {
			// This is the default, so we don't need to render anything
		}
		else {
			if ( needsWhitespace ) {
				hql.append( ' ' );
			}
			switch ( mode ) {
				case GROUPS:
					hql.append( "groups " );
					break;
				case RANGE:
					hql.append( "range " );
					break;
				case ROWS:
					hql.append( "rows " );
					break;
			}
			if ( endKind == CURRENT_ROW ) {
				renderFrameKind( hql, startKind, startExpression, context );
			}
			else {
				hql.append( "between " );
				renderFrameKind( hql, startKind, startExpression, context );
				hql.append( " and " );
				renderFrameKind( hql, endKind, endExpression, context );
			}
			switch ( exclusion ) {
				case TIES:
					hql.append( " exclude ties" );
					break;
				case CURRENT_ROW:
					hql.append( " exclude current row" );
					break;
				case GROUP:
					hql.append( " exclude group" );
					break;
			}
		}
	}

	private static void renderFrameKind(StringBuilder sb, FrameKind kind, SqmExpression<?> expression, SqmRenderContext context) {
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
				expression.appendHqlString( sb, context );
				sb.append( " preceding" );
				break;
			case OFFSET_FOLLOWING:
				expression.appendHqlString( sb, context );
				sb.append( " following" );
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported frame kind: " + kind );
		}
	}

	@Override
	public boolean equals(Object object) {
		if ( !(object instanceof SqmWindow sqmWindow) ) {
			return false;
		}
		return Objects.equals( partitions, sqmWindow.partitions )
			&& Objects.equals( orderList, sqmWindow.orderList )
			&& mode == sqmWindow.mode
			&& startKind == sqmWindow.startKind
			&& Objects.equals( startExpression, sqmWindow.startExpression )
			&& endKind == sqmWindow.endKind
			&& Objects.equals( endExpression, sqmWindow.endExpression )
			&& exclusion == sqmWindow.exclusion;
	}

	@Override
	public int hashCode() {
		return Objects.hash( partitions, orderList, mode, startKind, startExpression, endKind, endExpression,
				exclusion );
	}
}
