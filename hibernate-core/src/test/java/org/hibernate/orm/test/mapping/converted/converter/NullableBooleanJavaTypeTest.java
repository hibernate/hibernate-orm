package org.hibernate.orm.test.mapping.converted.converter;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-10371")
@SessionFactory
@DomainModel(annotatedClasses = { NullableBooleanJavaTypeTest.Car.class })
public class NullableBooleanJavaTypeTest {

	@Test
	public void testFalse(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Car car = new Car();

					car.setId( 1L );
					car.setName( "TestCar" );
					car.setMainCar( Boolean.FALSE );
					car.setMainCars( Boolean.FALSE );

					session.persist( car );
				}
		);
		scope.inSession(
				session -> {
					final Car entity = session.find( Car.class, 1L );
					assertEquals( "TestCar", entity.name );
					assertFalse( entity.mainCar );
					assertFalse( entity.mainCars );
				}
		);
	}

	@Test
	public void testTrue(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Car car = new Car();

					car.setId( 2L );
					car.setName( "MainCar" );
					car.setMainCar( Boolean.TRUE );
					car.setMainCars( Boolean.TRUE );

					session.persist( car );
				}
		);
		scope.inSession(
				session -> {
					final Car entity = session.find( Car.class, 2L );
					assertEquals( "MainCar", entity.name );
					assertTrue( entity.mainCar );
					assertTrue( entity.mainCars );
				}
		);
	}

	@Converter
	public static class BooleanLongConverter implements AttributeConverter<Boolean, Long> {

		private static final Long LONG_TRUE = 1L;
		private static final Long LONG_FALSE = null;

		@Override
		public Long convertToDatabaseColumn(Boolean attribute) {
			return attribute ? LONG_TRUE : LONG_FALSE;
		}

		@Override
		public Boolean convertToEntityAttribute(Long dbData) {
			return LONG_TRUE.equals( dbData ) ? Boolean.TRUE : Boolean.FALSE;
		}
	}

	@Converter
	public static class BooleanStringConverter implements AttributeConverter<Boolean, String> {

		private static final String STRING_TRUE = "1";
		private static final String STRING_FALSE = null;

		@Override
		public String convertToDatabaseColumn(Boolean attribute) {
			return attribute ? STRING_TRUE : STRING_FALSE;
		}

		@Override
		public Boolean convertToEntityAttribute(String dbData) {
			return STRING_TRUE.equals( dbData ) ? Boolean.TRUE : Boolean.FALSE;
		}
	}

	@Entity
	@Table(name = "car")
	public static class Car implements Serializable {

		@Id
		private Long id;

		private String name;

		@Convert(converter = BooleanLongConverter.class)
		@Column(columnDefinition = "NUMERIC(38,0)")
		private Boolean mainCar;

		@Convert(converter = BooleanStringConverter.class)
		@Column(columnDefinition = "VARCHAR(38)")
		private Boolean mainCars;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Boolean getMainCar() {
			return mainCar;
		}

		public void setMainCar(Boolean mainCar) {
			this.mainCar = mainCar;
		}

		public Boolean getMainCars() {
			return mainCars;
		}

		public void setMainCars(Boolean mainCars) {
			this.mainCars = mainCars;
		}
	}
}
