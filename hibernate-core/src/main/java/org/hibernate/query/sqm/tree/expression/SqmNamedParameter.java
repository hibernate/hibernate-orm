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
 * Represents a named query parameter in the SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmNamedParameter implements SqmParameter {
	private final String name;
	private final boolean canBeMultiValued;
	private AllowableParameterType expressableType;

	public SqmNamedParameter(String name, boolean canBeMultiValued) {
		this.name = name;
		this.canBeMultiValued = canBeMultiValued;
	}

	public SqmNamedParameter(String name, boolean canBeMultiValued, AllowableParameterType expressableType) {
		this.name = name;
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
	public void impliedType(ExpressableType expressableType) {
		if ( expressableType != null ) {
			if ( expressableType instanceof AllowableParameterType ) {
				this.expressableType = (AllowableParameterType) expressableType;
			}
			else {
				throw new IllegalArgumentException(
						"The implied type for named parameter [" +
								getName() + "] was not an AllowableParameterType - " + expressableType
				);
			}
		}
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
	public Integer getPosition() {
		return null;
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
		return expressableType.getJavaTypeDescriptor();
	}
}
