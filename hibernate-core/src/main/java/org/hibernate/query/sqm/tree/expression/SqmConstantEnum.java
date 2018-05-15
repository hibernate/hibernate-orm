/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmConstantEnum<T extends Enum> implements SqmConstantReference<T> {
	private final T value;
	private BasicValuedExpressableType domainType;

	public SqmConstantEnum(T value) {
		this( value, null );
	}

	public SqmConstantEnum(T value, BasicValuedExpressableType domainType) {
		this.value = value;
		this.domainType = domainType;
	}

	@Override
	public T getLiteralValue() {
		return value;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return domainType;
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void impliedType(ExpressableType expressableType) {
		this.domainType = (BasicValuedExpressableType) expressableType;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitConstantEnumExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "EnumConstant(" + value + ")";
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return domainType.getJavaTypeDescriptor();
	}
}
