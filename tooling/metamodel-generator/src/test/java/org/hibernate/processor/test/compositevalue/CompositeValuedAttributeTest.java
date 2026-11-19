package org.hibernate.processor.test.compositevalue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.TemporalAttribute;
import jakarta.persistence.metamodel.TextAttribute;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * An attribute mapped as a composite value is a plain {@code SingularAttribute}
 * at runtime, whatever its Java type, so its static metamodel field must not be
 * one of the specialized subtypes.
 */
@CompilationTest
@TestForIssue(jiraKey = "HHH-21120")
class CompositeValuedAttributeTest {
	@Test
	@WithClasses({ Event.class, Details.class, Money.class, ZonedDateTimeType.class, DateTimeAndZoneType.class })
	void compositeValuedAttributesArePlainSingularAttributes() {
		assertAttribute( Event.class, "basic", TemporalAttribute.class, Event.class, ZonedDateTime.class );
		assertAttribute( Event.class, "composite", SingularAttribute.class, Event.class, ZonedDateTime.class );
		assertAttribute( Event.class, "metaComposite", SingularAttribute.class, Event.class, ZonedDateTime.class );
		// the time zone storage strategy doesn't affect the metamodel type
		assertAttribute( Event.class, "column", TemporalAttribute.class, Event.class, OffsetDateTime.class );
		assertAttribute( Event.class, "auto", TemporalAttribute.class, Event.class, OffsetDateTime.class );
		assertAttribute( Event.class, "normalized", TemporalAttribute.class, Event.class, OffsetDateTime.class );
		assertAttribute( Event.class, "price", SingularAttribute.class, Event.class, Money.class );
		assertAttribute( Event.class, "details", SingularAttribute.class, Event.class, Details.class );
		assertAttribute( Details.class, "note", TextAttribute.class, Details.class );
		assertAttribute( Details.class, "shippedAt", SingularAttribute.class, Details.class, ZonedDateTime.class );
	}

	private static void assertAttribute(
			Class<?> type, String fieldName, Class<?> expectedRawType, Type... expectedTypeArguments) {
		final var field = getFieldFromMetamodelFor( type, fieldName );
		final var parameterizedType = assertInstanceOf( ParameterizedType.class, field.getGenericType() );
		assertEquals( expectedRawType, parameterizedType.getRawType() );
		assertArrayEquals( expectedTypeArguments, parameterizedType.getActualTypeArguments() );
	}
}
