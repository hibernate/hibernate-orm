/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.type.descriptor.java.internal.JdbcDateJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.JdbcTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.JdbcTimestampJavaDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class LiteralHelper {
	public static final SqmLiteral<Integer> INTEGER_ZERO = new SqmLiteral<>( 0, StandardSpiBasicTypes.INTEGER );
	public static final SqmLiteral<Integer> INTEGER_ONE = new SqmLiteral<>( 1, StandardSpiBasicTypes.INTEGER );

	public static SqmLiteral<Timestamp> timestampLiteralFrom(String literalText, SqmCreationState creationState) {
		final Timestamp literal = Timestamp.valueOf(
				LocalDateTime.from( JdbcTimestampJavaDescriptor.FORMATTER.parse( literalText ) )
		);

		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getDomainModel().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Timestamp.class )
		);
	}

	public static SqmLiteral<Date> dateLiteralFrom(String literalText, SqmCreationState creationState) {
		final LocalDate localDate = LocalDate.from( JdbcDateJavaDescriptor.FORMATTER.parse( literalText ) );
		final Date literal = new Date( localDate.toEpochDay() );

		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getDomainModel().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Date.class )
		);
	}

	public static SqmLiteral<Time> timeLiteralFrom(String literalText, SqmCreationState creationState) {
		final LocalTime localTime = LocalTime.from( JdbcTimeJavaDescriptor.FORMATTER.parse( literalText ) );
		final Time literal = Time.valueOf( localTime );

		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getDomainModel().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Time.class )
		);
	}
}
