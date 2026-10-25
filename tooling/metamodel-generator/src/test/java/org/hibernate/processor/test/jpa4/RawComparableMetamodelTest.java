package org.hibernate.processor.test.jpa4;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.persistence.metamodel.ComparableAttribute;
import jakarta.persistence.metamodel.NumericAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.TemporalAttribute;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@CompilationTest
@TestForIssue(jiraKey = "HHH-11931")
class RawComparableMetamodelTest {
	@Test
	@WithClasses(RawComparableEntity.class)
	void rawComparableAttributesRetainTheirValueTypes() {
		assertAttribute( RawComparableEntity.class, "direct", SingularAttribute.class,
				RawComparableEntity.RawValue.class );
		assertAttribute( RawComparableEntity.class, "inherited", SingularAttribute.class,
				RawComparableEntity.InheritedRawValue.class );
	}

	@Test
	@WithClasses(ParameterizedComparableEntity.class)
	void parameterizedComparableAttributesRetainTheirSpecialization() throws Exception {
		assertAttribute( ParameterizedComparableEntity.class, "value", ComparableAttribute.class,
				ParameterizedComparableEntity.class.getDeclaredField( "value" ).getGenericType() );
		assertAttribute( ParameterizedComparableEntity.class, "inherited", ComparableAttribute.class,
				ParameterizedComparableEntity.Child.class );
	}

	@Test
	@WithClasses(ParameterizedComparableEntity.class)
	void parameterizedNumericAttributeRetainsItsSpecialization() throws Exception {
		assertAttribute( ParameterizedComparableEntity.class, "number", NumericAttribute.class,
				ParameterizedComparableEntity.class.getDeclaredField( "number" ).getGenericType() );
	}

	@Test
	@WithClasses(ParameterizedComparableEntity.class)
	void parameterizedTemporalAttributeRetainsItsSpecialization() throws Exception {
		assertAttribute( ParameterizedComparableEntity.class, "temporal", TemporalAttribute.class,
				ParameterizedComparableEntity.class.getDeclaredField( "temporal" ).getGenericType() );
	}

	@Test
	@WithClasses(RawComparableEntity.class)
	void rawComparableNumericAttributeRetainsItsValueType() {
		assertAttribute( RawComparableEntity.class, "number", SingularAttribute.class,
				RawComparableEntity.RawNumber.class );
	}

	@Test
	@WithClasses(RawComparableEntity.class)
	void rawComparableTemporalAttributeRetainsItsValueType() {
		assertAttribute( RawComparableEntity.class, "temporal", SingularAttribute.class,
				RawComparableEntity.RawTemporal.class );
	}

	private static void assertAttribute(
			Class<?> entityType, String fieldName, Class<?> expectedRawType, Type expectedValueType) {
		final var field = getFieldFromMetamodelFor( entityType, fieldName );
		final var parameterizedType = assertInstanceOf( ParameterizedType.class, field.getGenericType() );
		assertEquals( expectedRawType, parameterizedType.getRawType() );
		assertArrayEquals( new Type[] { entityType, expectedValueType }, parameterizedType.getActualTypeArguments() );
	}
}
