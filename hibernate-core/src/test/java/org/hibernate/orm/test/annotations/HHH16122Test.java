/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

@JiraKey( value = "HHH-16122" )
@Jpa( annotatedClasses = {HHH16122Test.ValueConverter.class, HHH16122Test.SuperClass.class, HHH16122Test.SubClass.class} )
public class HHH16122Test {

	@Test
	public void testGenericSuperClassWithConverter(EntityManagerFactoryScope scope) {
		// The test is successful if the entity manager factory can be built.
	}

	public static class ConvertedValue {
		public final long value;
		public ConvertedValue(long value) {
			this.value = value;
		}
	}

	@Converter(autoApply = true)
	public static class ValueConverter implements AttributeConverter<ConvertedValue, Long> {
		@Override
		public Long convertToDatabaseColumn( ConvertedValue value ) {
			return value.value;
		}
		@Override
		public ConvertedValue convertToEntityAttribute( Long value ) {
			return new ConvertedValue(value);
		}
	}

	@MappedSuperclass
	public static abstract class SuperClass<S extends SuperClass> {
		@Id
		private String id;
		public ConvertedValue convertedValue = new ConvertedValue( 1 );
		public ConvertedValue getConvertedValue() {
			return convertedValue;
		}
		public void setConvertedValue(ConvertedValue convertedValue) {
			this.convertedValue = convertedValue;
		}
	}

	@Entity(name = "SubClass")
	public static class SubClass extends SuperClass<SubClass> {}
}
