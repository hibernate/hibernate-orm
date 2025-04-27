/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Represents the reference to a Map attribute's {@link Map.Entry} entries
 * in a select clause
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
public class SqmMapEntryReference<K,V>
		implements SqmSelectableNode<Map.Entry<K,V>>, Expression<Map.Entry<K,V>>, SqmExpressible<Map.Entry<K,V>> {
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private final SqmPath<?> mapPath;
	private final NodeBuilder nodeBuilder;

	private final JavaType<Map.Entry<K,V>> mapEntryTypeDescriptor;

	private String explicitAlias;

	public SqmMapEntryReference(
			SqmPath<?> mapPath,
			NodeBuilder nodeBuilder) {
		this.mapPath = mapPath;
		this.nodeBuilder = nodeBuilder;

		this.mapEntryTypeDescriptor =
				nodeBuilder.getTypeConfiguration().getJavaTypeRegistry()
						.getDescriptor( Map.Entry.class );
	}

	@Override
	public SqmMapEntryReference<K, V> copy(SqmCopyContext context) {
		final SqmMapEntryReference<K, V> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		else {
			return context.registerCopy( this,
					new SqmMapEntryReference<>( mapPath.copy( context ), nodeBuilder() ) );
		}
	}

	@Override
	public String getAlias() {
		return explicitAlias;
	}

	public SqmPath<?> getMapPath() {
		return mapPath;
	}

	@Override
	public JpaSelection<Map.Entry<K, V>> alias(String name) {
		this.explicitAlias = name;
		return this;
	}

	@Override
	public JavaType<Map.Entry<K, V>> getJavaTypeDescriptor() {
		return mapEntryTypeDescriptor;
	}

	@Override
	public JavaType<Map.Entry<K, V>> getNodeJavaType() {
		return mapEntryTypeDescriptor;
	}

	@Override
	public JavaType<Map.Entry<K, V>> getExpressibleJavaType() {
		return mapEntryTypeDescriptor;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitMapEntryFunction( this );
	}

	@Override
	public void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer) {
		jpaSelectionConsumer.accept( this );
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return Collections.emptyList();
	}

	@Override
	public SqmExpressible<Map.Entry<K, V>> getNodeType() {
		return this;
	}

	@Override
	public SqmDomainType<Map.Entry<K, V>> getSqmType() {
		return null;
	}

	@Override
	public Class<Map.Entry<K, V>> getBindableJavaType() {
		return getNodeType().getBindableJavaType();
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "entry(" );
		mapPath.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmMapEntryReference<?, ?> that
			&& Objects.equals( mapPath, that.mapPath )
			&& Objects.equals( explicitAlias, that.explicitAlias );
	}

	@Override
	public int hashCode() {
		return Objects.hash( mapPath, explicitAlias );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA (ugh)

	@Override
	public Predicate isNull() {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate isNotNull() {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate equalTo(Expression<?> value) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate equalTo(Object value) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate notEqualTo(Expression<?> value) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate notEqualTo(Object value) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate in(Object... values) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate in(Expression<?>... values) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate in(Collection<?> values) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate in(Expression<Collection<?>> values) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public <X> Expression<X> as(Class<X> type) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public <X> Expression<X> cast(Class<X> type) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}
}
