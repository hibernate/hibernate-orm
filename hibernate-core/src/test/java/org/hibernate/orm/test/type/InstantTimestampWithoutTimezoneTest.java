/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.TimeZone;

import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = { InstantTimestampWithoutTimezoneTest.SomeEntity.class }
)
@SessionFactory
@JiraKey( "HHH-16317" )
public class InstantTimestampWithoutTimezoneTest {

	@Test
	@Disabled("Just for local testing")
	public void testJdbc(SessionFactoryScope scope) {
		TimeZone timeZone = TimeZone.getDefault();
		try {
			TimeZone.setDefault( TimeZone.getTimeZone( "GMT+1" ) );
			SharedDriverManagerConnectionProviderImpl.getInstance().reset();
			scope.inTransaction(
					session -> {
						session.doWork(
								connection -> {
									try (PreparedStatement preparedStatement = connection.prepareStatement(
											"insert into SOMEENTITY (ID,TSDATA) values (1,?)" )) {
										preparedStatement.setObject(
												1,
												OffsetDateTime.parse( "2020-01-01T12:00:00Z" )
										);
										preparedStatement.executeUpdate();
									}
								}
						);
					}
			);
			scope.inTransaction(
					session -> {
						session.doWork(
								connection -> {
									try (PreparedStatement preparedStatement = connection.prepareStatement(
											"select e.TSDATA from SOMEENTITY e where e.ID=1" );
										 ResultSet resultSet = preparedStatement.executeQuery()) {
										resultSet.next();
										OffsetDateTime offsetDateTime = resultSet.getObject( 1, OffsetDateTime.class );
										assertEquals(
												OffsetDateTime.parse( "2020-01-01T12:00:00Z" ).toInstant(),
												offsetDateTime.toInstant()
										);
									}
								}
						);
					}
			);
		}
		finally {
			TimeZone.setDefault( timeZone );
			SharedDriverManagerConnectionProviderImpl.getInstance().reset();
		}
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		TimeZone timeZone = TimeZone.getDefault();
		try {
			TimeZone.setDefault( TimeZone.getTimeZone( "GMT+1" ) );
			SharedDriverManagerConnectionProviderImpl.getInstance().reset();
			scope.inTransaction(
					session -> {
						session.createNativeMutationQuery( "insert into SOMEENTITY (ID,TSDATA) values (2,?)" )
								.setParameter( 1, OffsetDateTime.parse( "2020-01-01T12:00:00Z" ).toInstant() )
								.executeUpdate();
						final Instant instant = session.createNativeQuery(
								"select e.TSDATA from SOMEENTITY e where e.ID=2",
								Instant.class
						).getSingleResult();
						assertEquals(
								OffsetDateTime.parse( "2020-01-01T12:00:00Z" ).toInstant(),
								instant
						);
					}
			);
		}
		finally {
			TimeZone.setDefault( timeZone );
			SharedDriverManagerConnectionProviderImpl.getInstance().reset();
		}
	}

	@Entity
	@Table(name = "SOMEENTITY")
	@Access(AccessType.FIELD)
	public static class SomeEntity {
		@Id
		@Column(name = "ID")
		private Integer id;
		@Column(name = "TSDATA")
		private java.sql.Timestamp tsData;
	}
}
