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

					session.persist( car );
				}
		);
		scope.inSession(
				session -> {
					final Car entity = session.find( Car.class, 1L );
					assertEquals( "TestCar", entity.name );
					assertFalse( entity.mainCar );
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

					session.persist( car );
				}
		);
		scope.inSession(
				session -> {
					final Car entity = session.find( Car.class, 2L );
					assertEquals( "MainCar", entity.name );
					assertTrue( entity.mainCar );
				}
		);
	}

	@Converter
	public static class BooleanConverter implements AttributeConverter<Boolean, Long> {

		private static final Long LONG_TRUE = 1L;
		private static final Long LONG_FALSE = null;

		@Override
		public Long convertToDatabaseColumn(Boolean attribute) {
			return attribute ? LONG_TRUE : LONG_FALSE;
		}

		@Override
		public Boolean convertToEntityAttribute(Long dbData) {
			return LONG_TRUE == dbData ? Boolean.TRUE : Boolean.FALSE;
		}
	}

	@Entity
	@Table(name = "CAR")
	public static class Car implements Serializable {

		@Id
		@Column(name = "id")
		private Long id;

		private String name;

		@Convert(converter = BooleanConverter.class)
		@Column(name = "MAIN_CAR", columnDefinition = "NUMBER(38,0)")
		private Boolean mainCar;

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
	}
}
