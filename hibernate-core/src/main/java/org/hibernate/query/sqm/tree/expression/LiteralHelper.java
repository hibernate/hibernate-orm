/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static org.hibernate.type.StandardBasicTypes.*;
import org.hibernate.query.spi.QueryEngine;

/**
 * @author Steve Ebersole
 */
public class LiteralHelper {

	/**
	 * Recognizes timestamps consisting of a date and time separated
	 * by either T or a space, and with an optional offset or time
	 * zone ID. Ideally we should accept both ISO and SQL standard
	 * zoned timestamp formats here.
	 */
	private static final DateTimeFormatter DATE_TIME = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.append( ISO_LOCAL_DATE )
			.optionalStart().appendLiteral( ' ' ).optionalEnd()
			.optionalStart().appendLiteral( 'T' ).optionalEnd()
			.append( ISO_LOCAL_TIME )
			.optionalStart().appendLiteral( ' ' ).optionalEnd()
			.optionalStart().appendZoneOrOffsetId().optionalEnd()
			.toFormatter();

	public static SqmLiteral<?> timestampLiteralFrom(String literalText, SqmCreationState creationState) {
		TemporalAccessor parsed = DATE_TIME.parse( literalText );
		try {
			ZonedDateTime zonedDateTime = ZonedDateTime.from( parsed );
			Calendar literal = GregorianCalendar.from( zonedDateTime );
			return new SqmLiteral<Calendar>(
					literal,
					creationState.getCreationContext().getDomainModel().getTypeConfiguration().resolveStandardBasicType( CALENDAR ),
					creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
			) {
				@Override
				protected void internalApplyInferableType(ExpressableType newType) {}
			};
		}
		catch (DateTimeException dte) {
			LocalDateTime localDateTime = LocalDateTime.from( parsed );
			Timestamp literal = Timestamp.valueOf( localDateTime );
			return new SqmLiteral<>(
					literal,
					creationState.getCreationContext().getDomainModel().getTypeConfiguration().resolveStandardBasicType( TIMESTAMP ),
					creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
			);
		}
	}

	public static SqmLiteral<Integer> integerLiteral(String literalText, SqmCreationState creationState) {
		return integerLiteral( literalText, creationState.getCreationContext().getQueryEngine() );
	}

	public static SqmLiteral<Integer> integerLiteral(String literalText, QueryEngine queryEngine) {
		return integerLiteral( Integer.parseInt( literalText ), queryEngine );
	}

	public static SqmLiteral<Integer> integerLiteral(int value, QueryEngine queryEngine) {
		return new SqmLiteral<>(
				value,
				StandardSpiBasicTypes.INTEGER,
				queryEngine.getCriteriaBuilder()
		);
	}

	public static SqmLiteral<Date> dateLiteralFrom(String literalText, SqmCreationState creationState) {
		final LocalDate localDate = LocalDate.from( ISO_LOCAL_DATE.parse( literalText ) );
		final Date literal = Date.valueOf( localDate );
		return new SqmLiteral(
				literal,
				creationState.getCreationContext().getDomainModel().getTypeConfiguration().resolveStandardBasicType( DATE ),
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}

	public static SqmLiteral<Time> timeLiteralFrom(String literalText, SqmCreationState creationState) {
		final LocalTime localTime = LocalTime.from( ISO_LOCAL_TIME.parse( literalText ) );
		final Time literal = Time.valueOf( localTime );
		return new SqmLiteral(
				literal,
				creationState.getCreationContext().getDomainModel().getTypeConfiguration().resolveStandardBasicType( TIME ),
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}
}
