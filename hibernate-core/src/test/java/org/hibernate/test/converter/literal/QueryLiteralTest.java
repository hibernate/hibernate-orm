/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.literal;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Janario Oliveira
 */
public class QueryLiteralTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			EntityConverter.class, NumberIntegerConverter.class, NumberStringConverter.class,
			StringWrapperConverter.class, IntegerWrapperConverter.class
		};
	}

	@Test
	public void testIntegerWrapper() {
		EntityConverter entity = new EntityConverter();
		entity.setIntegerWrapper( new IntegerWrapper( 10 ) );
		save( entity );

		entity = find( entity.getId(), "e.integerWrapper=10" );

		assertNotNull( entity );
		assertEquals( 10, entity.getIntegerWrapper().getValue() );
	}

	@Test
	public void testIntegerWrapperThrowsException() {
		EntityConverter entity = new EntityConverter();
		entity.setIntegerWrapper( new IntegerWrapper( 10 ) );
		save( entity );

		try {
			find( entity.getId(), "e.integerWrapper='10'" );
			fail("Should throw QueryException!");
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
			assertTrue( e.getMessage().contains( "AttributeConverter domain-model attribute type [org.hibernate.test.converter.literal.QueryLiteralTest$IntegerWrapper] and JDBC type [java.lang.Integer] did not match query literal type [java.lang.String]" ) );
		}
	}

	@Test
	public void testStringWrapper() {
		EntityConverter entity = new EntityConverter();
		entity.setStringWrapper( new StringWrapper( "TEN" ) );
		save( entity );

		entity = find( entity.getId(), "e.stringWrapper='TEN'" );

		assertNotNull( entity );
		assertEquals( "TEN", entity.getStringWrapper().getValue() );
	}

	@Test
	public void testSameTypeConverter() {
		EntityConverter entity = new EntityConverter();
		entity.setSameTypeConverter( "HUNDRED" );
		save( entity );

		entity = find( entity.getId(), "e.sameTypeConverter='HUNDRED'" );

		assertNotNull( entity );
		assertEquals( "HUNDRED", entity.getSameTypeConverter() );

		Session session = openSession();
		String value = (String) session.createSQLQuery( "select e.same_type_converter from entity_converter e where e.id=:id" )
				.setParameter( "id", entity.getId() )
				.uniqueResult();
		assertEquals( "VALUE_HUNDRED", value );
		session.close();
	}

	@Test
	public void testEnumOrdinal() {
		EntityConverter entity = new EntityConverter();
		entity.setLetterOrdinal( Letter.B );
		save( entity );

		entity = find( entity.getId(), "e.letterOrdinal=" + Letter.B.ordinal() );

		assertNotNull( entity );
		assertEquals( Letter.B, entity.getLetterOrdinal() );
	}

	@Test
	public void testEnumString() {
		EntityConverter entity = new EntityConverter();
		entity.setLetterString( Letter.C );
		save( entity );

		entity = find( entity.getId(), "e.letterString='" + Letter.C.name() + "'" );

		assertNotNull( entity );
		assertEquals( Letter.C, entity.getLetterString() );
	}

	@Test
	public void testNumberImplicit() {
		EntityConverter entity = new EntityConverter();
		entity.setNumbersImplicit( Numbers.THREE );
		save( entity );

		entity = find( entity.getId(), "e.numbersImplicit=" + ( Numbers.THREE.ordinal() + 1 ) );

		assertNotNull( entity );
		assertEquals( Numbers.THREE, entity.getNumbersImplicit() );
	}

	@Test
	public void testNumberImplicitOverrided() {
		EntityConverter entity = new EntityConverter();
		entity.setNumbersImplicitOverrided( Numbers.TWO );
		save( entity );

		entity = find( entity.getId(), "e.numbersImplicitOverrided='" + ( Numbers.TWO.ordinal() + 1 ) + "'" );

		assertNotNull( entity );
		assertEquals( Numbers.TWO, entity.getNumbersImplicitOverrided() );
	}

	private void save(EntityConverter entity) {
		Session session = openSession();
		session.beginTransaction();
		session.persist( entity );

		session.getTransaction().commit();
		session.close();
	}

	private EntityConverter find(int id, String queryLiteral) {
		Session session = openSession();
		Query query = session.createQuery(
				"select e from EntityConverter e where e.id=:id and " + queryLiteral );
		query.setParameter( "id", id );
		EntityConverter entity = (EntityConverter) query.uniqueResult();
		session.close();
		return entity;
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
		private Numbers numbersImplicitOverrided;

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

		public Numbers getNumbersImplicitOverrided() {
			return numbersImplicitOverrided;
		}

		public void setNumbersImplicitOverrided(Numbers numbersImplicitOverrided) {
			this.numbersImplicitOverrided = numbersImplicitOverrided;
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
