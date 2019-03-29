/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * Represents a named query parameter in the SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmNamedParameter extends AbstractSqmParameter {
	private final String name;

	public SqmNamedParameter(String name, boolean canBeMultiValued) {
		this( name, canBeMultiValued, null );
	}

	public SqmNamedParameter(String name, boolean canBeMultiValued, AllowableParameterType inherentType) {
		super( canBeMultiValued, inherentType );
		this.name = name;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitNamedParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return ":" + getName();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SqmParameter copy() {
		return new SqmNamedParameter( getName(), allowMultiValuedBinding() );
	}
}
