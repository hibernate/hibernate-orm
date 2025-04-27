/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import java.util.Objects;

/**
 * Needed to pass TrimSpecification as an SqmExpression when we call out to
 * SqmFunctionTemplates handling TRIM calls as a function argument.
 *
 * @author Steve Ebersole
 */
public class SqmTrimSpecification extends AbstractSqmNode implements SqmTypedNode<Void> {
	private final TrimSpec specification;

	public SqmTrimSpecification(TrimSpec specification, NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.specification = specification;
	}

	@Override
	public SqmTrimSpecification copy(SqmCopyContext context) {
		return this;
	}

	public TrimSpec getSpecification() {
		return specification;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitTrimSpecification( this );
	}

	@Override
	public String asLoggableText() {
		return specification.name();
	}

	@Override
	public SqmExpressible<Void> getNodeType() {
		return null;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( specification );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmTrimSpecification that
			&& specification == that.specification;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( specification );
	}
}
