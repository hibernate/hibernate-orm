/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.literal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.ExceptionHelper;
import org.hibernate.query.Query;
import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Janario Oliveira
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				QueryLiteralTest.EntityConverter.class,
				QueryLiteralTest.NumberIntegerConverter.class,
				QueryLiteralTest.NumberStringConverter.class,
				QueryLiteralTest.StringWrapperConverter.class,
				QueryLiteralTest.IntegerWrapperConverter.class
		}
)
@SessionFactory
public class QueryLiteralTest {
	@Test
	public void testIntegerWrapper(SessionFactoryScope scope) {
		final EntityConverter created = scope.fromTransaction(
				(session) -> {
					EntityConverter entity = new EntityConverter();
					entity.setIntegerWrapper( new IntegerWrapper( 10 ) );
					session.persist( entity );

					return entity;
				}
		);

		final EntityConverter loaded = scope.fromTransaction(
				(session) -> find( created.id, "e.integerWrapper=10", session )
		);

		assertNotNull( loaded );
		assertEquals( 10, created.getIntegerWrapper().getValue() );
	}

	@Test
	public void testIntegerWrapperThrowsException(SessionFactoryScope scope) {
		final EntityConverter created = scope.fromTransaction(
				(session) -> {
					EntityConverter entity = new EntityConverter();
					entity.setIntegerWrapper( new IntegerWrapper( 10 ) );
					session.persist( entity );

					return entity;
				}
		);

		try {
			scope.fromTransaction(
					(session) -> find( created.id, "e.integerWrapper = '10'", session )
			);
			fail( "Should throw Exception!" );
		}
		catch (Exception e) {
			assertThat( ExceptionHelper.getRootCause( e ), instanceOf( SemanticException.class ) );
		}
	}

	@Test
	public void testStringWrapper(SessionFactoryScope scope) {
		final EntityConverter created = scope.fromTransaction(
				(session) -> {
					EntityConverter entity = new EntityConverter();
					entity.setStringWrapper( new StringWrapper( "TEN" ) );
					session.persist( entity );

					return entity;
				}
		);

		final EntityConverter loaded = scope.fromTransaction(
				(session) -> find( created.id, "e.stringWrapper='TEN'", session )
		);

		assertNotNull( loaded );
		assertEquals( "TEN", loaded.getStringWrapper().getValue() );
	}

	@Test
	public void testSameTypeConverter(SessionFactoryScope scope) {
		final EntityConverter created = scope.fromTransaction(
				(session) -> {
					EntityConverter entity = new EntityConverter();
					entity.setSameTypeConverter( "HUNDRED" );
					session.persist( entity );

					return entity;
				}
		);

		final EntityConverter loaded = scope.fromTransaction(
				(session) -> find( created.id, "e.sameTypeConverter='HUNDRED'", session )
		);

		assertNotNull( loaded );
		assertEquals( "HUNDRED", loaded.getSameTypeConverter() );

		scope.inTransaction(
				(session) -> {
					String value = (String) session.createNativeQuery( "select e.same_type_converter from entity_converter e where e.id=:id" )
							.setParameter( "id", loaded.getId() )
							.uniqueResult();
					assertEquals( "VALUE_HUNDRED", value );
				}
		);
	}

	@Test
	public void testEnumOrdinal(SessionFactoryScope scope) {
		final EntityConverter created = scope.fromTransaction(
				(session) -> {
					EntityConverter entity = new EntityConverter();
					entity.setLetterOrdinal( Letter.B );
					session.persist( entity );

					return entity;
				}
		);

		final EntityConverter loaded = scope.fromTransaction(
				(session) -> find( created.id, "e.letterOrdinal=" + Letter.B.ordinal(), session )
		);

		assertNotNull( loaded );
		assertEquals( Letter.B, loaded.getLetterOrdinal() );
	}

	@Test
	public void testEnumString(SessionFactoryScope scope) {
		final EntityConverter created = scope.fromTransaction(
				(session) -> {
					EntityConverter entity = new EntityConverter();
					entity.setLetterString( Letter.C );
					session.persist( entity );

					return entity;
				}
		);

		final EntityConverter loaded = scope.fromTransaction(
				(session) -> find( created.id, "e.letterString='" + Letter.C.name() + "'", session )
		);

		assertNotNull( loaded );
		assertEquals( Letter.C, loaded.getLetterString() );
	}

	@Test
	public void testNumberImplicit(SessionFactoryScope scope) {
		final EntityConverter created = scope.fromTransaction(
				(session) -> {
					EntityConverter entity = new EntityConverter();
					entity.setNumbersImplicit( Numbers.THREE );
					session.persist( entity );

					return entity;
				}
		);

		final EntityConverter loaded = scope.fromTransaction(
				(session) -> find( created.id, "e.numbersImplicit=" + ( Numbers.THREE.ordinal() + 1 ), session )
		);

		assertNotNull( loaded );
		assertEquals( Numbers.THREE, loaded.getNumbersImplicit() );
	}

	@Test
	public void testNumberImplicitOverridden(SessionFactoryScope scope) {
		final EntityConverter created = scope.fromTransaction(
				(session) -> {
					EntityConverter entity = new EntityConverter();
					entity.setNumbersImplicitOverridden( Numbers.TWO );
					session.persist( entity );

					return entity;
				}
		);

		final EntityConverter loaded = scope.fromTransaction(
				(session) -> find( created.id, "e.numbersImplicitOverridden='" + ( Numbers.TWO.ordinal() + 1 ) + "'", session )
		);

		assertNotNull( loaded );
		assertEquals( Numbers.TWO, loaded.getNumbersImplicitOverridden() );
	}

	private EntityConverter find(int id, String queryLiteral, SessionImplementor session) {
		final String qryBase = "select e from EntityConverter e where e.id=:id and ";
		final Query<EntityConverter> query = session.createQuery(qryBase + queryLiteral, EntityConverter.class );
		query.setParameter( "id", id );
		return query.uniqueResult();
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	public enum Letter {
		A, B, C
	}

	public enum Numbers {
		ONE, TWO, THREE
	}

	@Converter(autoApply = true)
	public static class NumberIntegerConverter implements AttributeConverter<Numbers, Integer> {
		@Override
		public Integer convertToDatabaseColumn(Numbers attribute) {
			return attribute == null ? null : attribute.ordinal() + 1;
		}

		@Override
		public Numbers convertToEntityAttribute(Integer dbData) {
			return dbData == null ? null : Numbers.values()[dbData - 1];
		}
	}

	@Converter
	public static class NumberStringConverter implements AttributeConverter<Numbers, String> {
		@Override
		public String convertToDatabaseColumn(Numbers attribute) {
			return attribute == null ? null : Integer.toString( attribute.ordinal() + 1 );
		}

		@Override
		public Numbers convertToEntityAttribute(String dbData) {
			return dbData == null ? null : Numbers.values()[Integer.parseInt( dbData ) - 1];
		}
	}


	@Converter(autoApply = true)
	public static class IntegerWrapperConverter implements AttributeConverter<IntegerWrapper, Integer> {
		@Override
		public Integer convertToDatabaseColumn(IntegerWrapper attribute) {
			return attribute == null ? null : attribute.getValue();
		}

		@Override
		public IntegerWrapper convertToEntityAttribute(Integer dbData) {
			return dbData == null ? null : new IntegerWrapper( dbData );
		}
	}

	@Converter(autoApply = true)
	public static class StringWrapperConverter implements AttributeConverter<StringWrapper, String> {
		@Override
		public String convertToDatabaseColumn(StringWrapper attribute) {
			return attribute == null ? null : attribute.getValue();
		}

		@Override
		public StringWrapper convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new StringWrapper( dbData );
		}
	}

	@Converter
	public static class PreFixedStringConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute == null ? null : "VALUE_" + attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData == null ? null : dbData.substring( 6 );
		}
	}

	@Entity(name = "EntityConverter")
	@Table(name = "entity_converter")
	public static class EntityConverter {
		@Id
		@GeneratedValue
		private Integer id;

		private Letter letterOrdinal;
		@Enumerated(EnumType.STRING)
		private Letter letterString;

		private Numbers numbersImplicit;
		@Convert(converter = NumberStringConverter.class)
		private Numbers numbersImplicitOverridden;

		private IntegerWrapper integerWrapper;
		private StringWrapper stringWrapper;

		@Convert(converter = PreFixedStringConverter.class)
		@Column(name = "same_type_converter")
		private String sameTypeConverter;

		public Integer getId() {
			return id;
		}

		public Letter getLetterOrdinal() {
			return letterOrdinal;
		}

		public void setLetterOrdinal(Letter letterOrdinal) {
			this.letterOrdinal = letterOrdinal;
		}

		public Letter getLetterString() {
			return letterString;
		}

		public void setLetterString(Letter letterString) {
			this.letterString = letterString;
		}

		public Numbers getNumbersImplicit() {
			return numbersImplicit;
		}

		public void setNumbersImplicit(Numbers numbersImplicit) {
			this.numbersImplicit = numbersImplicit;
		}

		public Numbers getNumbersImplicitOverridden() {
			return numbersImplicitOverridden;
		}

		public void setNumbersImplicitOverridden(Numbers numbersImplicitOverridden) {
			this.numbersImplicitOverridden = numbersImplicitOverridden;
		}

		public IntegerWrapper getIntegerWrapper() {
			return integerWrapper;
		}

		public void setIntegerWrapper(IntegerWrapper integerWrapper) {
			this.integerWrapper = integerWrapper;
		}

		public StringWrapper getStringWrapper() {
			return stringWrapper;
		}

		public void setStringWrapper(StringWrapper stringWrapper) {
			this.stringWrapper = stringWrapper;
		}

		public String getSameTypeConverter() {
			return sameTypeConverter;
		}

		public void setSameTypeConverter(String sameTypeConverter) {
			this.sameTypeConverter = sameTypeConverter;
		}
	}

	public static class IntegerWrapper {
		private final int value;

		public IntegerWrapper(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		@Override
		public String toString() {
			return String.format( "IntegerWrapper{value=%d}", value);
		}
	}

	public static class StringWrapper {
		private final String value;

		public StringWrapper(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
}
