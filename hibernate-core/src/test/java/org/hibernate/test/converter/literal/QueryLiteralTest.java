/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.literal;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.hibernate.Query;
import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Janario Oliveira
 */
public class QueryLiteralTest extends BaseNonConfigCoreFunctionalTestCase {

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
		String value = (String) session.createSQLQuery(
				"select e.same_type_converter from entity_converter e where e.id=:id" )
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityConverter.class, NumberIntegerConverter.class, NumberStringConverter.class,
				StringWrapperConverter.class, IntegerWrapperConverter.class
		};
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

}
