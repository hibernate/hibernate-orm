/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.Selection;

/**
 * @asciidoctor
 *
 * Models either a `Tuple` or `Object[]` selection as defined by the
 * JPA Criteria API.
 *
 * @see org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder#tuple(Selection[])
 * @see jakarta.persistence.criteria.CriteriaBuilder#tuple(jakarta.persistence.criteria.Selection[])
 *
 * @see org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder#array(Selection[])
 * @see jakarta.persistence.criteria.CriteriaBuilder#array(jakarta.persistence.criteria.Selection[])
 *
 * @see org.hibernate.query.sqm.tree.expression.SqmTuple
 *
 * @author Steve Ebersole
 */
public class SqmJpaCompoundSelection<T>
		extends AbstractSqmExpression<T>
		implements JpaCompoundSelection<T>, SqmExpressible<T> {

	private final List<? extends SqmSelectableNode<?>> selectableNodes;
	private final JavaType<T> javaType;

	public SqmJpaCompoundSelection(
			List<? extends SqmSelectableNode<?>> selectableNodes,
			JavaType<T> javaType,
			NodeBuilder criteriaBuilder) {
		super( null, criteriaBuilder );
		this.selectableNodes = selectableNodes;
		this.javaType = javaType;

//		setExpressibleType( this );
	}

	@Override
	public SqmJpaCompoundSelection<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmSelectableNode<?>> selectableNodes = new ArrayList<>( this.selectableNodes.size() );
		for ( SqmSelectableNode<?> selectableNode : this.selectableNodes ) {
			selectableNodes.add( selectableNode.copy( context ) );
		}
		return context.registerCopy(
				this,
				new SqmJpaCompoundSelection<>(
						selectableNodes,
						javaType,
						nodeBuilder()
				)
		);
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return javaType;
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

//	@Override
//	public Class<T> getJavaType() {
//		return getJavaType();
//	}

	@Override
	public List<? extends SqmSelectableNode<?>> getSelectionItems() {
		return selectableNodes;
	}

	@Override
	public JpaSelection<T> alias(String name) {
		return this;
	}

	@Override
	public @Nullable String getAlias() {
		return null;
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitJpaCompoundSelection( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		appendSelectableNode( hql, context, selectableNodes.get( 0 ) );
		for ( int i = 1; i < selectableNodes.size(); i++ ) {
			hql.append(", ");
			appendSelectableNode( hql, context, selectableNodes.get( i ) );
		}
	}

	private static void appendSelectableNode(StringBuilder hql, SqmRenderContext context, SqmSelectableNode<?> sqmSelectableNode) {
		sqmSelectableNode.appendHqlString( hql, context );
		if ( sqmSelectableNode.getAlias() != null ) {
			hql.append( " as " ).append( sqmSelectableNode.getAlias() );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if ( !(object instanceof SqmJpaCompoundSelection<?> that)
			|| selectableNodes.size() != that.selectableNodes.size() ) {
			return false;
		}
		for ( SqmSelectableNode<?> selectableNode : selectableNodes ) {
			if ( !selectableNode.equals( that )
				|| !Objects.equals( selectableNode.getAlias(), that.getAlias() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( selectableNodes );
	}

	@Override
	public boolean isCompatible(Object object) {
		if ( !(object instanceof SqmJpaCompoundSelection<?> that)
			|| selectableNodes.size() != that.selectableNodes.size() ) {
			return false;
		}
		for ( SqmSelectableNode<?> selectableNode : selectableNodes ) {
			if ( !selectableNode.isCompatible( that )
				|| !Objects.equals( selectableNode.getAlias(), that.getAlias() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int cacheHashCode() {
		return SqmCacheable.cacheHashCode( selectableNodes );
	}

	@Override
	public void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer) {
		selectableNodes.forEach( jpaSelectionConsumer );
	}

	@Override
	public @Nullable SqmDomainType<T> getSqmType() {
		return null;
	}
}
