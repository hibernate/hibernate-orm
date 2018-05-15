/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.sql.Time;
import java.time.LocalTime;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.internal.JdbcTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralTime implements SqmLiteral<Time>, ImpliedTypeSqmExpression {
	public static SqmLiteralTime from(String literalText, SqmCreationContext creationContext) {
		final LocalTime localTime = LocalTime.from( JdbcTimeJavaDescriptor.FORMATTER.parse( literalText ) );
		final Time literal = Time.valueOf( localTime );

		return new SqmLiteralTime(
				literal,
				creationContext.getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Time.class )
		);
	}

	private final Time value;

	private BasicValuedExpressableType type;

	public SqmLiteralTime(Time value, BasicValuedExpressableType type) {
		this.value = value;
		this.type = type;
	}

	@Override
	public Time getLiteralValue() {
		return value;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return type;
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return type;
	}

	@Override
	public void impliedType(ExpressableType type) {
		this.type = (BasicValuedExpressableType) type;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLiteralTimeExpression( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
