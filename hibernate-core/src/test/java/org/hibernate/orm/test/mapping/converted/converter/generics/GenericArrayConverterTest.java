package org.hibernate.orm.test.mapping.converted.converter.generics;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that auto-applied converters whose domain type is a generic array
 * ({@code T[]}) declared in an abstract superclass only match attributes
 * of the concrete array type.
 */
@JiraKey("HHH-20662")
@DomainModel(
		annotatedClasses = {
				GenericArrayConverterTest.TestEntity.class,
				GenericArrayConverterTest.StringArrayConverter.class,
				GenericArrayConverterTest.IntegerArrayConverter.class
		}
)
@SessionFactory
public class GenericArrayConverterTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testGenericArrayConverters(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist(
				new TestEntity( 1L, new String[] {"a", "b"}, new Integer[] {1, 2} )
		) );

		scope.inSession( session -> {
			final Object[] row = session.createNativeQuery(
					"select strings, integers from TestEntity where id = 1", Object[].class
			).getSingleResult();
			assertThat( row[0] ).isEqualTo( "a,b" );
			assertThat( row[1] ).isEqualTo( "1,2" );
		} );

		scope.inTransaction( session -> {
			final TestEntity entity = session.find( TestEntity.class, 1L );
			assertThat( entity.strings ).containsExactly( "a", "b" );
			assertThat( entity.integers ).containsExactly( 1, 2 );
		} );
	}

	public static abstract class AbstractArrayConverter<T> implements AttributeConverter<T[], String> {
		@Override
		public String convertToDatabaseColumn(T[] attribute) {
			return attribute == null
					? null
					: Arrays.stream( attribute ).map( String::valueOf ).collect( Collectors.joining( "," ) );
		}
	}

	@Converter(autoApply = true)
	public static class StringArrayConverter extends AbstractArrayConverter<String> {
		@Override
		public String[] convertToEntityAttribute(String dbData) {
			return dbData == null ? null : dbData.split( "," );
		}
	}

	@Converter(autoApply = true)
	public static class IntegerArrayConverter extends AbstractArrayConverter<Integer> {
		@Override
		public Integer[] convertToEntityAttribute(String dbData) {
			return dbData == null
					? null
					: Arrays.stream( dbData.split( "," ) ).map( Integer::valueOf ).toArray( Integer[]::new );
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String[] strings;

		private Integer[] integers;

		public TestEntity() {
		}

		public TestEntity(Long id, String[] strings, Integer[] integers) {
			this.id = id;
			this.strings = strings;
			this.integers = integers;
		}
	}
}
