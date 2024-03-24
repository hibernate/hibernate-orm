package org.hibernate.processor.test.records;

import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static java.lang.reflect.Modifier.isStatic;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Java14RecordsTest extends CompilationTest {

    @Test
    @TestForIssue(jiraKey = "HHH-16261")
    @WithClasses({Address.class, Author.class})
    public void testEmbeddableRecordProperty() {
        assertMetamodelClassGeneratedFor(Address.class);
        for (final String fieldName : List.of("street", "city", "postalCode")) {
            assertNotNull("Address must contain '" + fieldName + "' field", getFieldFromMetamodelFor(Address.class, fieldName));
        }
        assertMetamodelClassGeneratedFor(Author.class);

        final Field addressField = getFieldFromMetamodelFor(Author.class, "address");
        assertNotNull("Author must contain 'address' field", addressField);
        assertTrue(isStatic(addressField.getModifiers()));
        if (addressField.getGenericType() instanceof ParameterizedType parameterizedType) {
            assertEquals(SingularAttribute.class, parameterizedType.getRawType());
            final Type[] typeArguments = parameterizedType.getActualTypeArguments();
            assertEquals(2, typeArguments.length);
            assertEquals(Author.class, typeArguments[0]);
            assertEquals(Address.class, typeArguments[1]);
        } else {
            fail("Address field must be instance of ParameterizedType");
        }

        final Field addressNameField = getFieldFromMetamodelFor(Author.class, "address".toUpperCase());
        assertNotNull("Author must contain 'ADDRESS' field", addressNameField);
        assertTrue(isStatic(addressNameField.getModifiers()));
        assertEquals(String.class, addressNameField.getGenericType());
    }
}
