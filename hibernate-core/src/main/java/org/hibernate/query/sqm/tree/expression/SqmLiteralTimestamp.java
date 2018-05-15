/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.internal.JdbcTimestampJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralTimestamp implements SqmLiteral<Timestamp>, ImpliedTypeSqmExpression {
	public static SqmLiteralTimestamp from(String literalText, SqmCreationContext creationContext) {
		final Timestamp literal = Timestamp.valueOf(
				LocalDateTime.from( JdbcTimestampJavaDescriptor.FORMATTER.parse( literalText ) )
		);

		return new SqmLiteralTimestamp(
				literal,
				creationContext.getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Timestamp.class )
		);
	}

	private final Timestamp value;

	private BasicValuedExpressableType type;

	public SqmLiteralTimestamp(Timestamp value, BasicValuedExpressableType rawType) {
		this.value = value;
		this.type = rawType;
	}

	@Override
	public Timestamp getLiteralValue() {
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
		return walker.visitLiteralTimestampExpression( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return type.getJavaTypeDescriptor();
	}
}
