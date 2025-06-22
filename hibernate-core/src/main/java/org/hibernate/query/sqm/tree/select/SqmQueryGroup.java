/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaQueryGroup;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A grouped list of queries connected through a certain set operator.
 *
 * @author Christian Beikov
 */
public class SqmQueryGroup<T> extends SqmQueryPart<T> implements JpaQueryGroup<T> {

	private final List<SqmQueryPart<T>> queryParts;
	private SetOperator setOperator;

	public SqmQueryGroup(SqmQueryPart<T> queryPart) {
		this( queryPart.nodeBuilder(), null, CollectionHelper.listOf( queryPart ) );
	}

	public SqmQueryGroup(
			NodeBuilder nodeBuilder,
			SetOperator setOperator,
			List<SqmQueryPart<T>> queryParts) {
		super( nodeBuilder );
		this.setOperator = setOperator;
		this.queryParts = queryParts;
	}

	@Override
	public SqmQueryPart<T> copy(SqmCopyContext context) {
		final SqmQueryGroup<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmQueryPart<T>> queryParts = new ArrayList<>( this.queryParts.size() );
		for ( SqmQueryPart<T> queryPart : this.queryParts ) {
			queryParts.add( queryPart.copy( context ) );
		}
		final SqmQueryGroup<T> queryGroup = context.registerCopy(
				this,
				new SqmQueryGroup<>(
						nodeBuilder(),
						setOperator,
						queryParts
				)
		);
		copyTo( queryGroup, context );
		return queryGroup;
	}

	public List<SqmQueryPart<T>> queryParts() {
		return queryParts;
	}

	@Override
	public SqmQuerySpec<T> getFirstQuerySpec() {
		return queryParts.get( 0 ).getFirstQuerySpec();
	}

	@Override
	public SqmQuerySpec<T> getLastQuerySpec() {
		return queryParts.get( queryParts.size() - 1 ).getLastQuerySpec();
	}

	@Override
	public boolean isSimpleQueryPart() {
		return setOperator == null && queryParts.size() == 1 && queryParts.get( 0 ).isSimpleQueryPart();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQueryGroup( this );
	}

	@Override
	public List<SqmQueryPart<T>> getQueryParts() {
		return Collections.unmodifiableList( queryParts );
	}

	public SetOperator getSetOperator() {
		return setOperator;
	}

	public void setSetOperator(SetOperator setOperator) {
		if ( setOperator == null ) {
			throw new IllegalArgumentException();
		}
		this.setOperator = setOperator;
	}


	@Override
	public SqmQueryGroup<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		super.setSortSpecifications( sortSpecifications );
		return this;
	}

	@Override
	public SqmQueryGroup<T> setOffset(JpaExpression<? extends Number> offset) {
		super.setOffset( offset );
		return this;
	}

	@Override
	public SqmQueryGroup<T> setFetch(JpaExpression<? extends Number> fetch) {
		super.setFetch( fetch );
		return this;
	}

	@Override
	public SqmQueryGroup<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		super.setFetch( fetch, fetchClauseType );
		return this;
	}

	@Override
	public void validateQueryStructureAndFetchOwners() {
		final SqmQuerySpec<T> firstQuerySpec = getFirstQuerySpec();
		// We only need to validate the first query spec regarding fetch owner,
		// because the fetch structure must match in all query parts of the group which we validate next
		firstQuerySpec.validateFetchOwners();
		final List<SqmSelection<?>> firstSelections = firstQuerySpec.getSelectClause().getSelections();
		final int firstSelectionSize = firstSelections.size();
		final List<SqmTypedNode<?>> typedNodes = new ArrayList<>( firstSelectionSize );
		for ( int i = 0; i < firstSelectionSize; i++ ) {
			typedNodes.add( firstSelections.get( i ).getSelectableNode() );
		}
		validateQueryGroupFetchStructure( typedNodes );
	}

	private void validateQueryGroupFetchStructure(List<? extends SqmTypedNode<?>> typedNodes) {
		final int firstSelectionSize = typedNodes.size();
		for ( int i = 0; i < queryParts.size(); i++ ) {
			final SqmQueryPart<T> queryPart = queryParts.get( i );
			if ( queryPart instanceof SqmQueryGroup<?> ) {
				( (SqmQueryGroup<?>) queryPart ).validateQueryGroupFetchStructure( typedNodes );
			}
			else {
				final SqmQuerySpec<?> querySpec = (SqmQuerySpec<?>) queryPart;
				final List<SqmSelection<?>> selections = querySpec.getSelectClause().getSelections();
				if ( firstSelectionSize != selections.size() ) {
					throw new SemanticException( "All query parts in a query group must have the same arity" );
				}
				for ( int j = 0; j < firstSelectionSize; j++ ) {
					final SqmTypedNode<?> firstSqmSelection = typedNodes.get( j );
					final JavaType<?> firstJavaType = firstSqmSelection.getNodeJavaType();
					final JavaType<?> nodeJavaType = selections.get( j ).getNodeJavaType();
					if ( nodeJavaType != null && firstJavaType != null && firstJavaType != nodeJavaType ) {
						throw new SemanticException(
								"Select items of the same index must have the same java type across all query parts"
						);
					}
					if ( firstSqmSelection instanceof SqmFrom<?, ?> firstFrom ) {
						final SqmFrom<?, ?> from = (SqmFrom<?, ?>) selections.get( j ).getSelectableNode();
						validateFetchesMatch( firstFrom, from );
					}
				}
			}
		}
	}

	private void validateFetchesMatch(SqmFrom<?, ?> firstFrom, SqmFrom<?, ?> from) {
		final Iterator<? extends SqmJoin<?, ?>> firstJoinIter = firstFrom.getSqmJoins().iterator();
		final Iterator<? extends SqmJoin<?, ?>> joinIter = from.getSqmJoins().iterator();
		while ( firstJoinIter.hasNext() ) {
			final SqmJoin<?, ?> firstSqmJoin = firstJoinIter.next();
			if ( firstSqmJoin instanceof SqmAttributeJoin<?, ?> firstAttrJoin ) {
				if ( firstAttrJoin.isFetched() ) {
					SqmAttributeJoin<?, ?> matchingAttrJoin = null;
					while ( joinIter.hasNext() ) {
						final SqmJoin<?, ?> sqmJoin = joinIter.next();
						if ( sqmJoin instanceof SqmAttributeJoin<?, ?> attrJoin ) {
							if ( attrJoin.isFetched() ) {
								matchingAttrJoin = attrJoin;
								break;
							}
						}
					}
					if ( matchingAttrJoin == null || firstAttrJoin.getModel() != matchingAttrJoin.getModel() ) {
						throw new SemanticException(
								"All query parts in a query group must have the same join fetches in the same order"
						);
					}
					validateFetchesMatch( firstAttrJoin, matchingAttrJoin );
				}
			}
		}
		// At this point, the other iterator should only contain non-fetch joins
		while ( joinIter.hasNext() ) {
			final SqmJoin<?, ?> sqmJoin = joinIter.next();
			if ( sqmJoin instanceof SqmAttributeJoin<?, ?> attrJoin ) {
				if ( attrJoin.isFetched() ) {
					throw new SemanticException(
							"All query parts in a query group must have the same join fetches in the same order"
					);
				}
			}
		}
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		appendQueryPart( queryParts.get( 0 ), hql, context );
		for ( int i = 1; i < queryParts.size(); i++ ) {
			hql.append( ' ' );
			hql.append( setOperator.sqlString() );
			hql.append( ' ' );
			appendQueryPart( queryParts.get( i ), hql, context );
		}
		super.appendHqlString( hql, context );
	}

	private static void appendQueryPart(SqmQueryPart<?> queryPart, StringBuilder sb, SqmRenderContext context) {
		final boolean needsParenthesis = !queryPart.isSimpleQueryPart();
		if ( needsParenthesis ) {
			sb.append( '(' );
		}
		queryPart.appendHqlString( sb, context );
		if ( needsParenthesis ) {
			sb.append( ')' );
		}
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmQueryGroup<?> that
			&& super.equals( that )
			&& this.setOperator == that.setOperator
			&& Objects.equals( this.queryParts, that.queryParts );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), queryParts, setOperator );
	}
}
