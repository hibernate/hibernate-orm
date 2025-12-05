/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
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

import static jakarta.persistence.metamodel.Type.PersistenceType.EMBEDDABLE;


/**
 * Represents the reference to a Map attribute's {@link Map.Entry} entries
 * in a select clause
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
public class SqmMapEntryReference<K,V>
		implements SqmSelectableNode<Map.Entry<K,V>>, Expression<Map.Entry<K,V>>, SqmBindableType<Map.Entry<K,V>> {
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private final SqmPath<?> mapPath;
	private final NodeBuilder nodeBuilder;

	private final JavaType<Map.Entry<K,V>> mapEntryTypeDescriptor;

	private @Nullable String explicitAlias;

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
	public @Nullable String getAlias() {
		return explicitAlias;
	}

	public SqmPath<?> getMapPath() {
		return mapPath;
	}

	@Override @SuppressWarnings("unchecked")
	public Class<Map.Entry<K, V>> getJavaType() {
		final Class<?> entryClass = Map.Entry.class;
		return (Class<Map.Entry<K, V>>) entryClass;
	}

	@Override
	public JpaSelection<Map.Entry<K, V>> alias(String name) {
		this.explicitAlias = name;
		return this;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return EMBEDDABLE;
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
	public SqmBindableType<Map.Entry<K, V>> getNodeType() {
		return this;
	}

	@Override
	public @Nullable SqmDomainType<Map.Entry<K, V>> getSqmType() {
		return null;
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
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmMapEntryReference<?, ?> that
			&& mapPath.equals( that.mapPath )
			&& Objects.equals( explicitAlias, that.explicitAlias );
	}

	@Override
	public int hashCode() {
		int result = mapPath.hashCode();
		result = 31 * result + Objects.hashCode( explicitAlias );
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmMapEntryReference<?, ?> that
			&& mapPath.isCompatible( that.mapPath )
			&& Objects.equals( explicitAlias, that.explicitAlias );
	}

	@Override
	public int cacheHashCode() {
		int result = mapPath.cacheHashCode();
		result = 31 * result + Objects.hashCode( explicitAlias );
		return result;
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
