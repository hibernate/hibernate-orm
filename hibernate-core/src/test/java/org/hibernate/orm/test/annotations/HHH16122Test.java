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
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

@JiraKey( value = "HHH-16122" )
public class HHH16122Test extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ValueConverter.class, SuperClass.class, SubClass.class };
	}

	@Test
	public void testGenericSuperClassWithConverter() {
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
