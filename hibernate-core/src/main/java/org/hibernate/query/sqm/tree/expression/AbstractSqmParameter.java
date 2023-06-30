/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.BindableType;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;

/**
 * Common support for SqmParameter impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmParameter<T> extends AbstractSqmExpression<T> implements SqmParameter<T> {
	private final boolean canBeMultiValued;

	public AbstractSqmParameter(
			boolean canBeMultiValued,
			SqmExpressible<T> inherentType,
			NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.canBeMultiValued = canBeMultiValued;
	}

	@Override
	public void applyInferableType(SqmExpressible<?> type) {
		if ( type == null ) {
			return;
		}
		else if ( type instanceof PluralPersistentAttribute<?, ?, ?> ) {
			type = ( (PluralPersistentAttribute<?, ?, ?>) type ).getElementType();
		}
		internalApplyInferableType( type );
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return canBeMultiValued;
	}

	@Override
	public BindableType<T> getAnticipatedType() {
		return this.getNodeType();
	}

	@Override
	public Class<T> getParameterType() {
		return this.getNodeType().getExpressibleJavaType().getJavaTypeClass();
	}
}
