/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.converter.inheritence;

import java.util.List;
import jakarta.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the ability to interpret and understand AttributeConverter impls when the base class does not
 * explicitly implement AttributeConverter but implements it via an interface or superclass. This also
 * involves resolving any TypeVariables to Class or ParameterizedType.
 *
 * @author Svein Baardsen
 */
@JiraKey(value = "HHH-8854")
public class AttributeConverterOnSuperclassTest extends BaseUnitTestCase {

	public static class StringIntegerAttributeConverter implements AttributeConverter<String, Integer> {

		@Override
		public Integer convertToDatabaseColumn(String attribute) {
			return Integer.valueOf( attribute );
		}

		@Override
		public String convertToEntityAttribute(Integer dbData) {
			return String.valueOf( dbData );
		}
	}

	public static class StringIntegerConverterSubclass extends StringIntegerAttributeConverter {
	}

	@Test
	public void testAttributeConverterOnSuperclass() {
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl();
		try {
			final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
					StringIntegerConverterSubclass.class,
					bootstrapContext.getClassmateContext()
			);

			assertEquals( String.class, converterDescriptor.getDomainValueResolvedType().getErasedType() );
		}
		finally {
			bootstrapContext.close();
		}
	}

	public interface StringLongAttributeConverter extends AttributeConverter<String, Long> {
	}

	public static class StringLongAttributeConverterImpl implements StringLongAttributeConverter {

		@Override
		public Long convertToDatabaseColumn(String attribute) {
			return Long.valueOf( attribute );
		}

		@Override
		public String convertToEntityAttribute(Long dbData) {
			return String.valueOf( dbData );
		}
	}

	@Test
	public void testAttributeConverterOnInterface() {
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl();
		try {
			final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
					StringLongAttributeConverterImpl.class,
					bootstrapContext.getClassmateContext()
			);

			assertEquals( String.class, converterDescriptor.getDomainValueResolvedType().getErasedType() );
		}
		finally {
			bootstrapContext.close();
		}
	}

	public static class NoopAttributeConverter<T> implements AttributeConverter<T, T> {

		@Override
		public T convertToDatabaseColumn(T attribute) {
			return attribute;
		}

		@Override
		public T convertToEntityAttribute(T dbData) {
			return dbData;
		}
	}

	public static class StringNoopAttributeConverter extends NoopAttributeConverter<String> {
	}

	@Test
	public void testTypeVariableAttributeConverterTypeArguments() {
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl();
		try {
			final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
					StringNoopAttributeConverter.class,
					bootstrapContext.getClassmateContext()
			);

			assertEquals( String.class, converterDescriptor.getDomainValueResolvedType().getErasedType() );
		}
		finally {
			bootstrapContext.close();
		}
	}

	public static class ListNoopAttributeConverter<T> extends NoopAttributeConverter<List<T>> {
	}

	public static class StringListNoopAttributeConverter extends ListNoopAttributeConverter<String> {
	}

	@Test
	public void testParameterizedTypeWithTypeVariableAttributeConverterTypeArguments() {
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl();
		try {
			final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
					StringListNoopAttributeConverter.class,
					bootstrapContext.getClassmateContext()
			);

			assertEquals( List.class, converterDescriptor.getDomainValueResolvedType().getErasedType() );
		}
		finally {
			bootstrapContext.close();
		}
	}

}
