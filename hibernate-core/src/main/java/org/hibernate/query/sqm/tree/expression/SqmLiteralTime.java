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
import org.hibernate.type.descriptor.java.internal.JdbcTimeJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralTime extends AbstractSqmLiteral<Time> {
	public static SqmLiteralTime from(String literalText, SqmCreationContext creationContext) {
		final LocalTime localTime = LocalTime.from( JdbcTimeJavaDescriptor.FORMATTER.parse( literalText ) );
		final Time literal = Time.valueOf( localTime );

		return new SqmLiteralTime(
				literal,
				creationContext.getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Time.class )
		);
	}

	public SqmLiteralTime(Time value, BasicValuedExpressableType inherentType) {
		super( value, inherentType );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLiteralTimeExpression( this );
	}
}
