package org.hibernate.test.cfg;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeConverter;

import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * Tests creation of AttributeConverterDefinition instances and
 * the classes of the extracted type arguments
 * 
 * @author Svein Baardsen
 */
public class AttributeConverterDefinitionTest extends BaseUnitTestCase {
	
	public static class CustomAttributeConverter implements AttributeConverter<List<String>, Integer> {

		@Override
		public Integer convertToDatabaseColumn(List<String> attribute) {
			return attribute.size();
		}

		@Override
		public List<String> convertToEntityAttribute(Integer dbData) {
			return new ArrayList<String>(dbData);
		}
		
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8804")
	public void testGenericTypeParameters() {
		AttributeConverterDefinition def = AttributeConverterDefinition.from(CustomAttributeConverter.class);
		assertEquals(List.class, def.getEntityAttributeType());
	}

}
