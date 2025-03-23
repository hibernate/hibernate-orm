/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Query;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				AttributeConverterAndNullTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-17687")
public class AttributeConverterAndNullTest {

	@Test
	public void testSelectByConnvertedField(SessionFactoryScope scope) {
		TheField theField = new TheField( "field1" );
		TestEntity testEntity = new TestEntity( theField );
		scope.inTransaction(
				session -> {
					session.persist( testEntity );
				}
		);

		scope.inSession(
				session -> {
					Query query1 = session.createQuery( "FROM TestEntity WHERE myField = :myField" );
					query1.setParameter( "myField", theField );
					System.out.println( query1.getSingleResult() );
				}
		);

		TestEntity testEntity2 = new TestEntity( null );
		scope.inTransaction(
				session ->
						session.persist( testEntity2 )
		);

		scope.inSession(
				session -> {
					Query query2 = session.createQuery( "FROM TestEntity WHERE myField = :myField" );
					query2.setParameter( "myField", null );
					assertThat( query2.getResultList().size() ).isEqualTo( 1 );
				}
		);
	}

	public static class MyFieldConverter implements AttributeConverter<TheField, String> {

		@Override
		public String convertToDatabaseColumn(TheField myField) {
			if ( myField == null ) {
				return "null";
			}
			return myField.getaField();
		}

		@Override
		public TheField convertToEntityAttribute(String dbValue) {
			return new TheField( dbValue );
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private Long id;

		@Convert(converter = MyFieldConverter.class)
		private TheField myField;

		public TestEntity() {
		}

		public TestEntity(TheField myField) {
			this.myField = myField;
		}

		public Long getId() {
			return id;
		}

		public TheField getMyField() {
			return myField;
		}
	}


	public static class TheField {
		private String aField;

		public TheField() {
		}

		public TheField(String field1) {
			this.aField = field1;
		}

		public String getaField() {
			return aField;
		}

	}

}
