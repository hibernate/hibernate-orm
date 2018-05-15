/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models a positional parameter expression
 *
 * @author Steve Ebersole
 */
public class SqmPositionalParameter implements SqmParameter {
	private final int position;
	private final boolean canBeMultiValued;
	private AllowableParameterType expressableType;

	public SqmPositionalParameter(int position, boolean canBeMultiValued) {
		this.position = position;
		this.canBeMultiValued = canBeMultiValued;
	}

	public SqmPositionalParameter(int position, boolean canBeMultiValued, AllowableParameterType expressableType) {
		this.position = position;
		this.canBeMultiValued = canBeMultiValued;
		this.expressableType = expressableType;
	}

	@Override
	public AllowableParameterType getExpressableType() {
		return expressableType;
	}

	@Override
	public AllowableParameterType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPositionalParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "?" + getPosition();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public void impliedType(ExpressableType expressableType) {
		if ( expressableType != null ) {
			if ( expressableType instanceof AllowableParameterType ) {
				this.expressableType = (AllowableParameterType) expressableType;
			}
			else {
				throw new IllegalArgumentException(
						"The implied type for positional parameter [" +
								getPosition() + "] was not an AllowableParameterType - " + expressableType
				);
			}
		}
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return canBeMultiValued;
	}

	@Override
	public AllowableParameterType getAnticipatedType() {
		return getExpressableType();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
