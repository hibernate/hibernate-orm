package org.hibernate.test.type;

import static org.junit.Assert.assertEquals;

import javax.persistence.AttributeConverter;

import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * Test the ability to interpret and understand AttributeConverter impls when the base class does not
 * explicitly implement AttributeConverter but implements it via an interface or superclass.
 * 
 * @author Svein Baardsen
 */
@TestForIssue(jiraKey = "HHH-8854")
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
		AttributeConverterDefinition def = AttributeConverterDefinition.from( StringIntegerConverterSubclass.class );
		assertEquals( String.class, def.getEntityAttributeType() );
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
		AttributeConverterDefinition def = AttributeConverterDefinition.from( StringLongAttributeConverterImpl.class );
		assertEquals( String.class, def.getEntityAttributeType() );
	}

}
