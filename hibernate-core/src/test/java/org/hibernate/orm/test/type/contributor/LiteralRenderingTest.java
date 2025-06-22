/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ServiceRegistry
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@SessionFactory
@JiraKey(value = "HHH-15590")
public class LiteralRenderingTest {

	public static List<Object> literalValues() throws Exception {
		return List.<Object>of(
				true,
				(byte) 1,
				(short) 1,
				(int) 1,
				(long) 1,
				1f,
				1d,
				'c',
				"string",
				LocalDate.parse( "2000-01-01" ),
				LocalDateTime.parse( "2000-01-01T01:01:01" ),
				LocalTime.parse( "01:01:01" ),
				LocalDateTime.parse( "2000-01-01T01:01:01" ).toInstant( ZoneOffset.UTC ),
				Date.valueOf( LocalDate.parse( "2000-01-01" ) ),
				Timestamp.valueOf( LocalDateTime.parse( "2000-01-01T01:01:01" ) ),
				Time.valueOf( LocalTime.parse( "01:01:01" ) ),
				java.util.Date.from( LocalDateTime.parse( "2000-01-01T01:01:01" ).toInstant( ZoneOffset.UTC ) ),
				GregorianCalendar.from( LocalDateTime.parse( "2000-01-01T01:01:01" ).atZone( ZoneOffset.UTC ) ),
				LocalDateTime.parse( "2000-01-01T01:01:01" ).atZone( ZoneOffset.UTC ),
				LocalDateTime.parse( "2000-01-01T01:01:01" ).atOffset( ZoneOffset.UTC ),
				LocalTime.parse( "01:01:01" ).atOffset( ZoneOffset.UTC ),
				new char[]{ 'c' },
				new byte[]{ 1 },
				new Character[]{ 'c' },
				new Byte[]{ 1 },
				Locale.ENGLISH,
				UUID.fromString( "53886a8a-7082-4879-b430-25cb94415be8" ),
				ZoneId.of( "UTC" ),
				BigDecimal.ONE,
				BigInteger.ONE,
				Year.of( 2000 ),
				TimeZone.getTimeZone( "UTC" ),
				Currency.getInstance( "EUR" ),
				Duration.ofDays( 1 ),
				InetAddress.getByName( "127.0.0.1" ),
				new URL( "https://hibernate.org" ),
				Boolean.class
		);
	}

	@ParameterizedTest
	@MethodSource("literalValues")
	public void testIdVersionFunctions(Object literalValue, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<Object> query = cb.createQuery();
					query.from( BasicEntity.class );
					query.select( cb.literal( 1 ) );
					query.where( cb.isNotNull( cb.literal( literalValue ) ) );
					session.createQuery( query ).list();
				}
		);
	}

}
