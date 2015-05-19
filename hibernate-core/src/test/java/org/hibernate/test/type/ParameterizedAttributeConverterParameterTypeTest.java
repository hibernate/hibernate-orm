/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.AttributeConverter;

import org.hibernate.cfg.AttributeConverterDefinition;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the ability to interpret and understand AttributeConverter impls which
 * use parameterized types as one of (typically the "attribute type") its parameter types.
 * 
 * @author Svein Baardsen
 * @author Steve Ebersole
 */
public class ParameterizedAttributeConverterParameterTypeTest extends BaseUnitTestCase {

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
		AttributeConverterDefinition def = AttributeConverterDefinition.from( CustomAttributeConverter.class );
		assertEquals( List.class, def.getEntityAttributeType() );
	}

}
