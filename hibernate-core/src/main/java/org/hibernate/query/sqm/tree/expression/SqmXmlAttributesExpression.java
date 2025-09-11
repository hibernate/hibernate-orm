/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.expression.XmlAttributes;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Special expression for the json_query function that also captures special syntax elements like error and empty behavior.
 *
 * @since 7.0
 */
@Incubating
public class SqmXmlAttributesExpression implements SqmTypedNode<Object> {

	private final Map<String, SqmExpression<?>> attributes;

	public SqmXmlAttributesExpression(String attributeName, Expression<?> expression) {
		final Map<String, SqmExpression<?>> attributes = new LinkedHashMap<>();
		attributes.put( attributeName, (SqmExpression<?>) expression );
		this.attributes = attributes;
	}

	private SqmXmlAttributesExpression(Map<String, SqmExpression<?>> attributes) {
		this.attributes = attributes;
	}

	public void attribute(String attributeName, Expression<?> expression) {
		attributes.put( attributeName, (SqmExpression<?>) expression );
	}

	public Map<String, SqmExpression<?>> getAttributes() {
		return attributes;
	}

	@Override
	public @Nullable SqmBindableType<Object> getNodeType() {
		return null;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		final Map<String, org.hibernate.sql.ast.tree.expression.Expression> attributes = new LinkedHashMap<>();
		for ( Map.Entry<String, SqmExpression<?>> entry : this.attributes.entrySet() ) {
			attributes.put( entry.getKey(), (org.hibernate.sql.ast.tree.expression.Expression) entry.getValue().accept( walker ) );
		}
		//noinspection unchecked
		return (X) new XmlAttributes( attributes );
	}

	@Override
	public SqmXmlAttributesExpression copy(SqmCopyContext context) {
		final SqmXmlAttributesExpression existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final Map<String, SqmExpression<?>> attributes = new LinkedHashMap<>();
		for ( Map.Entry<String, SqmExpression<?>> entry : this.attributes.entrySet() ) {
			attributes.put( entry.getKey(), entry.getValue().copy( context ) );
		}
		return context.registerCopy( this, new SqmXmlAttributesExpression( attributes ) );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		String separator = "xmlattributes(";
		for ( Map.Entry<String, SqmExpression<?>> entry : attributes.entrySet() ) {
			hql.append( separator );
			entry.getValue().appendHqlString( hql, context );
			hql.append( " as " );
			hql.append( entry.getKey() );
			separator = ", ";
		}
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmXmlAttributesExpression that
			&& Objects.equals( attributes, that.attributes );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( attributes );
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmXmlAttributesExpression that
			&& SqmCacheable.areCompatible( attributes, that.attributes );
	}

	@Override
	public int cacheHashCode() {
		return SqmCacheable.cacheHashCode( attributes );
	}
}
