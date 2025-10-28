/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.io.Serializable;
import java.util.Date;
import java.util.stream.Stream;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10959")
@DomainModel( annotatedClasses = LongToDateConversionTest.TestEntity.class )
@SessionFactory
public class LongToDateConversionTest {

	@Test
	public void testSetParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qryStr = "SELECT e FROM TestEntity e WHERE e.date <= :ts";
					final Query<TestEntity> query = session.createQuery( qryStr, TestEntity.class );
					query.setParameter( "ts", new DateAttribute( System.currentTimeMillis() ) );
					final Stream<TestEntity> stream = query.stream();
					assertThat( stream.count(), is( 1L ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					TestEntity entity = new TestEntity();
					entity.setDate( new DateAttribute( System.currentTimeMillis() ) );
					session.persist( entity );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private long id;

		@Convert(converter = DateAttributeConverter.class)
		@Column(name = "attribute_date")
		private DateAttribute date;

		public DateAttribute getDate() {
			return date;
		}

		public void setDate(DateAttribute date) {
			this.date = date;
		}
	}

	public static class DateAttribute implements Serializable {
		private long field;

		public DateAttribute(long field) {
			this.field = field;
		}
	}

	public static class DateAttributeConverter implements AttributeConverter<DateAttribute, Date> {

		@Override
		public Date convertToDatabaseColumn(DateAttribute attribute) {
			if ( attribute == null ) {
				return null;
			}
			return new Date( attribute.field );
		}

		@Override
		public DateAttribute convertToEntityAttribute(Date dbData) {
			if ( dbData == null ) {
				return null;
			}
			return new DateAttribute( dbData.getTime() );
		}

	}
}
