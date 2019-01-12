/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Supplier;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractInferableTypeSqmExpression implements InferableTypeSqmExpression {
	private ExpressableType inherentType;
	private Supplier<? extends ExpressableType> typeInference;

	public AbstractInferableTypeSqmExpression(ExpressableType inherentType) {
		this.inherentType = inherentType;
	}

	protected void setInherentType(ExpressableType inherentType) {
		if ( this.inherentType == null ) {
			this.inherentType = inherentType;
		}
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}

	@Override
	public ExpressableType getExpressableType() {
		return getInferableType().get();
	}

	@Override
	public Supplier<? extends ExpressableType> getInferableType() {
		return () -> {
//			final ExpressableType inferredType;
//			if ( typeInference != null ) {
//				inferredType = typeInference.get();
//			}
//			else {
//				inferredType = null;
//			}
//
//			return firstNonStandardBasicType( inferredType, inherentType );
			return inherentType;
		};
	}

	private ExpressableType firstNonStandardBasicType(ExpressableType... types) {
		ExpressableType firstNonNull = null;

		for ( ExpressableType type : types ) {
			if ( type == null ) {
				continue;
			}

			if ( ! (type instanceof StandardSpiBasicTypes.StandardBasicType) ) {
				return type;
			}

			if ( firstNonNull == null ) {
				firstNonNull = type;
			}
		}

		// which might itself still be null..
		return firstNonNull;
	}

	@Override
	public void impliedType(Supplier<? extends ExpressableType> inference) {
		this.typeInference = inference;
	}
}
