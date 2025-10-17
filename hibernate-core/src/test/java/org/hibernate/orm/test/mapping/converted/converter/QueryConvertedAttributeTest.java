/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.time.Year;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@JiraKey( value = "HHH-15742")
@DomainModel(annotatedClasses = {QueryConvertedAttributeTest.EntityA.class})
@SessionFactory
public class QueryConvertedAttributeTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA1 = new EntityA( 1, Year.parse("2022") );
			EntityA entityA2 = new EntityA( 2, Year.parse("2021") );
			EntityA entityA3 = new EntityA( 3, null );
			s.persist( entityA1 );
			s.persist( entityA2 );
			s.persist( entityA3 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.createMutationQuery( "delete entitya" ).executeUpdate() );
	}

	@Test
	public void testQueryConvertedAttribute(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			TypedQuery<EntityA> query = s.createQuery( "from entitya" , EntityA.class);
			List<EntityA> resultList = query.getResultList();
			assertEquals(3, resultList.size());
		} );

		scope.inTransaction( s -> {
			TypedQuery<EntityA> query = s.createQuery(
					"from entitya a where :year is null or a.year = :year",
					EntityA.class
			);
			query.setParameter( "year", Year.parse( "2022" ) );

			List<EntityA> resultList = query.getResultList();
			assertEquals(1, resultList.size());
			assertEquals(1, resultList.get( 0 ).getId());
			assertEquals("2022", resultList.get( 0 ).getYear().toString());
		} );
	}

	@Entity(name = "entitya")
	public static class EntityA {
		@Id
		private Integer id;

		@Column(name = "year_attribute")
		@Convert(converter = YearConverter.class)
		private Year year;

		public EntityA() {
		}

		public EntityA(Integer id, Year year) {
			this.id = id;
			this.year = year;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Year getYear() {
			return year;
		}

		public void setYear(Year year) {
			this.year = year;
		}
	}

	@Converter
	public static class YearConverter implements AttributeConverter<Year, String> {
		@Override
		public String convertToDatabaseColumn(Year attribute) {
			return attribute != null ? attribute.toString() : null;
		}
		@Override
		public Year convertToEntityAttribute(String dbData) {
			return dbData != null ? Year.parse( dbData ) : null;
		}
	}
}
