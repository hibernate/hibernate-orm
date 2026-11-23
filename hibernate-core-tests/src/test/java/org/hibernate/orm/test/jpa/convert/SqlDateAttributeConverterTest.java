package org.hibernate.orm.test.jpa.convert;

import java.sql.Date;
import java.sql.Types;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies the String-to-java.sql.Date converter and column metadata reported in HHH-13360.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = SqlDateAttributeConverterTest.DateEntity.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-13360")
public class SqlDateAttributeConverterTest {
	@Test
	void testConvertedAndUnconvertedColumnsAreDates(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement();
					var result = statement.executeQuery( "select plain_date, converted_date from converted_sql_dates" ) ) {
				final var metadata = result.getMetaData();
				assertEquals( Types.DATE, metadata.getColumnType( 1 ) );
				assertEquals( "DATE", metadata.getColumnTypeName( 1 ) );
				assertEquals( Types.DATE, metadata.getColumnType( 2 ) );
				assertEquals( "DATE", metadata.getColumnTypeName( 2 ) );
			}
		} ) );
	}

	@Test
	void testStringToSqlDateRoundTrip(SessionFactoryScope scope) {
		final Date date = Date.valueOf( "2020-06-24" );
		final String value = Long.toString( date.getTime() );
		scope.inTransaction( session -> {
			final DateEntity entity = new DateEntity();
			entity.id = 1L;
			entity.plainDate = date;
			entity.convertedDate = value;
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final DateEntity entity = session.find( DateEntity.class, 1L );
			assertEquals( date, entity.plainDate );
			assertEquals( value, entity.convertedDate );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "DateEntity")
	@Table(name = "converted_sql_dates")
	public static class DateEntity {
		@Id
		private Long id;

		@Column(name = "plain_date")
		private Date plainDate;

		@Convert(converter = StringToSqlDateConverter.class)
		@Column(name = "converted_date")
		private String convertedDate;
	}

	@Converter
	public static class StringToSqlDateConverter implements AttributeConverter<String, Date> {
		@Override
		public Date convertToDatabaseColumn(String attribute) {
			return attribute == null ? null : new Date( Long.parseLong( attribute ) );
		}

		@Override
		public String convertToEntityAttribute(Date dbData) {
			return dbData == null ? null : Long.toString( dbData.getTime() );
		}
	}
}
