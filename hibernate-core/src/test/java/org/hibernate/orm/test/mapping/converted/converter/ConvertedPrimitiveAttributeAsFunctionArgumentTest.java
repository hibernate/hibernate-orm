/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ConvertedPrimitiveAttributeAsFunctionArgumentTest.PrimitiveEntity.class,
		ConvertedPrimitiveAttributeAsFunctionArgumentTest.WrapperEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17835" )
public class ConvertedPrimitiveAttributeAsFunctionArgumentTest {
	@Test
	public void testConvertedPrimitive(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select upper(convertedInt) from PrimitiveEntity",
					String.class
			).getSingleResult() ).isEqualTo( "1" );
			assertThat( session.createQuery(
					"select floor(convertedChar) from PrimitiveEntity",
					Character.class
			).getSingleResult() ).isEqualTo( '1' );
		} );
	}

	@Test
	public void testConvertedWrapper(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select upper(convertedInt) from WrapperEntity",
					String.class
			).getSingleResult() ).isEqualTo( "1" );
			assertThat( session.createQuery(
					"select floor(convertedChar) from WrapperEntity",
					Character.class
			).getSingleResult() ).isEqualTo( '1' );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new PrimitiveEntity( 1L, 1, '1' ) );
			session.persist( new WrapperEntity( 1L, 1, '1' ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from PrimitiveEntity" ).executeUpdate();
			session.createMutationQuery( "delete from WrapperEntity" ).executeUpdate();
		} );
	}


	@Entity( name = "PrimitiveEntity" )
	public static class PrimitiveEntity {
		@Id
		private long id;

		@Convert( converter = IntToStringConverter.class )
		private int convertedInt;

		@Convert( converter = CharToIntegerConverter.class )
		private char convertedChar;

		public PrimitiveEntity() {
		}

		public PrimitiveEntity(long id, int convertedInt, char convertedChar) {
			this.id = id;
			this.convertedInt = convertedInt;
			this.convertedChar = convertedChar;
		}
	}

	@Entity( name = "WrapperEntity" )
	public static class WrapperEntity {
		@Id
		private Long id;

		@Convert( converter = IntToStringConverter.class )
		private Integer convertedInt;

		@Convert( converter = CharToIntegerConverter.class )
		private Character convertedChar;

		public WrapperEntity() {
		}

		public WrapperEntity(Long id, Integer convertedInt, Character convertedChar) {
			this.id = id;
			this.convertedInt = convertedInt;
			this.convertedChar = convertedChar;
		}
	}

	@Converter
	public static class IntToStringConverter implements AttributeConverter<Integer, String> {
		@Override
		public String convertToDatabaseColumn(Integer integer) {
			return integer.toString();
		}

		@Override
		public Integer convertToEntityAttribute(String s) {
			return Integer.parseInt( s );
		}
	}

	@Converter
	public static class CharToIntegerConverter implements AttributeConverter<Character, Integer> {
		@Override
		public Integer convertToDatabaseColumn(Character character) {
			return (int) character;
		}

		@Override
		public Character convertToEntityAttribute(Integer integer) {
			return (char) integer.intValue();
		}
	}
}
