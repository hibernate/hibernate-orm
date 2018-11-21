/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.lang.reflect.Field;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * Represents a constant that came from a static field reference.
 *
 * @author Steve Ebersole
 */
public class SqmConstantFieldReference<T> extends AbstractSqmLiteral<T> implements SqmConstantReference<T> {
	private final Field sourceField;

	public SqmConstantFieldReference(Field sourceField, T value, BasicValuedExpressableType inherentType) {
		super( value, inherentType );
		this.sourceField = sourceField;
	}

	public Field getSourceField() {
		return sourceField;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitConstantFieldReference( this );
	}

	@Override
	public String asLoggableText() {
		return "ConstantField(" + getLiteralValue() + ")";
	}
}
