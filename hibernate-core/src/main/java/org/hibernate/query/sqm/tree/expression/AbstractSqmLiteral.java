/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmLiteral<T> implements SqmLiteral<T> {
	private final T value;

	private BasicValuedExpressableType type;

	public AbstractSqmLiteral(T value) {
		this.value = value;
	}

	public AbstractSqmLiteral(T value, BasicValuedExpressableType type) {
		this.value = value;
		this.type = type;
	}

	@Override
	public T getLiteralValue() {
		return value;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return type;
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void impliedType(ExpressableType type) {
		if ( type != null ) {
			if ( !BasicValuedExpressableType.class.isInstance( type ) ) {
				throw new SemanticException( "Inferrable type for literal was found to be a non-basic value : " + type );
			}
			this.type = (BasicValuedExpressableType) type;
		}
	}

	@Override
	public String asLoggableText() {
		return "Literal( " + value + ")";
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return type.getJavaTypeDescriptor();
	}
}
