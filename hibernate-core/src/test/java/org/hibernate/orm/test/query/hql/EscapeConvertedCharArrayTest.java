/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = EscapeConvertedCharArrayTest.Vehicle.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-16211")
public class EscapeConvertedCharArrayTest {
	private static final String STRING_PROP = "TEST123456";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Vehicle vehicle = new Vehicle( 1L, STRING_PROP.toCharArray(), STRING_PROP );
			session.persist( vehicle );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Vehicle" ).executeUpdate() );
	}

	@Test
	public void testCharArrayPropEscapeLiteral(SessionFactoryScope scope) {
		testLikeEscape( scope, "charArrayProp", false );
	}

	@Test
	public void testCharArrayPropEscapeParameter(SessionFactoryScope scope) {
		testLikeEscape( scope, "charArrayProp", true );
	}

	@Test
	public void testConvertedStringPropEscapeLiteral(SessionFactoryScope scope) {
		testLikeEscape( scope, "convertedStringProp", false );
	}

	@Test
	public void testConvertedStringPropEscapeParameter(SessionFactoryScope scope) {
		testLikeEscape( scope, "convertedStringProp", true );
	}

	private void testLikeEscape(SessionFactoryScope scope, String propertyName, boolean parameter) {
		scope.inTransaction( session -> {
			final TypedQuery<Vehicle> query = session.createQuery(
							String.format(
									"from Vehicle where %s like :param escape %s",
									propertyName,
									parameter ? ":escape" : "'!'"
							),
							Vehicle.class
					)
					.setParameter( "param", "TEST%" );
			if ( parameter ) {
				query.setParameter( "escape", '!' );
			}
			final Vehicle vehicle = query.getSingleResult();
			assertEquals( 1L, vehicle.getId() );
			assertArrayEquals( STRING_PROP.toCharArray(), vehicle.getCharArrayProp() );
			assertEquals( STRING_PROP, vehicle.getConvertedStringProp() );
		} );
	}

	@Entity(name = "Vehicle")
	public static class Vehicle implements Serializable {
		@Id
		private Long id;

		private char[] charArrayProp;

		@Convert(converter = StringToCharConverter.class)
		private String convertedStringProp;

		public Vehicle() {
		}

		public Vehicle(Long id, char[] charArrayProp, String convertedStringProp) {
			this.id = id;
			this.charArrayProp = charArrayProp;
			this.convertedStringProp = convertedStringProp;
		}

		public Long getId() {
			return id;
		}

		public char[] getCharArrayProp() {
			return charArrayProp;
		}

		public String getConvertedStringProp() {
			return convertedStringProp;
		}
	}

	@Converter
	public static class StringToCharConverter implements AttributeConverter<String, char[]> {
		@Override
		public char[] convertToDatabaseColumn(String attribute) {
			return attribute == null ? null : attribute.toCharArray();
		}

		@Override
		public String convertToEntityAttribute(char[] dbData) {
			return dbData == null ? null : new String( dbData );
		}
	}
}
