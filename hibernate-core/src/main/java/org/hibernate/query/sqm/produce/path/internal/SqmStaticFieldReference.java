/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmStaticFieldReference<T>
		extends AbstractSqmExpression<T>
		implements SemanticPathPart, SqmExpression<T> {
	private final Field referencedField;
	private final Object fieldValue;
	private final BasicJavaDescriptor<T> jtd;

	public SqmStaticFieldReference(Field referencedField, BasicJavaDescriptor<T> jtd, NodeBuilder nodeBuilder) {
		super( null, nodeBuilder );
		this.referencedField = referencedField;
		this.jtd = jtd;

		try {
			this.fieldValue = referencedField.get( null );
		}
		catch (IllegalAccessException e) {
			throw new SqmProductionException(
					"Could not access field value : `"
							+ referencedField.getDeclaringClass().getTypeName() + "."
							+ referencedField.getName() + "`",
					e
			);
		}

		setExpressableType( nodeBuilder.getTypeConfiguration().standardExpressableTypeForJavaType( jtd ) );
	}

	@Override
	public BasicValuedExpressableType<T> getExpressableType() {
		return (BasicValuedExpressableType<T>) super.getExpressableType();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {
		//noinspection StatementWithEmptyBody
		if ( newType instanceof BasicValuedNavigable ) {
			// basic valued navigable may have a ValueConverter associated
			final BasicValuedNavigable basicValuedNavigable = (BasicValuedNavigable) newType;
			setExpressableType( basicValuedNavigable );
		}
		else {
			// nothing to do
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFullyQualifiedField( referencedField );
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public SqmExpression<Long> asLong() {
		try {
			//noinspection unchecked
			return new SqmLiteral(
					referencedField.getLong( null ),
					getExpressableType(),
					nodeBuilder()
			);
		}
		catch (IllegalAccessException e) {
			throw new SqmProductionException(
					"Could not access field value : `"
							+ referencedField.getDeclaringClass().getTypeName() + "."
							+ referencedField.getName() + "`",
					e
			);
		}
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		try {
			//noinspection unchecked
			return new SqmLiteral(
					referencedField.getInt( null ),
					getExpressableType(),
					nodeBuilder()
			);
		}
		catch (IllegalAccessException e) {
			throw new SqmProductionException(
					"Could not access field value : `"
							+ referencedField.getDeclaringClass().getTypeName() + "."
							+ referencedField.getName() + "`",
					e
			);
		}
	}

	@Override
	public SqmExpression<Float> asFloat() {
		try {
			//noinspection unchecked
			return new SqmLiteral(
					referencedField.getFloat( null ),
					getExpressableType(),
					nodeBuilder()
			);
		}
		catch (IllegalAccessException e) {
			throw new SqmProductionException(
					"Could not access field value : `"
							+ referencedField.getDeclaringClass().getTypeName() + "."
							+ referencedField.getName() + "`",
					e
			);
		}
	}

	@Override
	public SqmExpression<Double> asDouble() {
		try {
			//noinspection unchecked
			return new SqmLiteral(
					referencedField.getDouble( null ),
					getExpressableType(),
					nodeBuilder()
			);
		}
		catch (IllegalAccessException e) {
			throw new SqmProductionException(
					"Could not access field value : `"
							+ referencedField.getDeclaringClass().getTypeName() + "."
							+ referencedField.getName() + "`",
					e
			);
		}
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		//noinspection unchecked
		return new SqmLiteral(
				fieldValue,
				getExpressableType(),
				nodeBuilder()
		);
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		//noinspection unchecked
		return new SqmLiteral(
				fieldValue,
				getExpressableType(),
				nodeBuilder()
		);
	}

	@Override
	public SqmExpression<String> asString() {
		//noinspection unchecked
		return new SqmLiteral(
				fieldValue.toString(),
				getExpressableType(),
				nodeBuilder()
		);
	}
}
