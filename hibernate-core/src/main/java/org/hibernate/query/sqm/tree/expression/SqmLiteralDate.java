/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.sql.Date;
import java.time.LocalDate;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.internal.JdbcDateJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralDate implements SqmLiteral<Date>, ImpliedTypeSqmExpression {
	public static SqmLiteralDate from(String literalText, SqmCreationContext creationContext) {
		final LocalDate localDate = LocalDate.from( JdbcDateJavaDescriptor.FORMATTER.parse( literalText ) );
		final Date literal = new Date( localDate.toEpochDay() );

		return new SqmLiteralDate(
				literal,
				creationContext.getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Date.class )
		);
	}

	private final Date value;

	private BasicValuedExpressableType type;

	public SqmLiteralDate(Date value, BasicValuedExpressableType type) {
		this.value = value;
		this.type = type;
	}

	@Override
	public Date getLiteralValue() {
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
		return walker.visitLiteralDateExpression( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
