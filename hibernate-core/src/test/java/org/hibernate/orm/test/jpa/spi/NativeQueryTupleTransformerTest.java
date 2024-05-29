package org.hibernate.orm.test.jpa.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jakarta.persistence.Tuple;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.jpa.spi.NativeQueryTupleTransformer;

import org.junit.Test;

/**
 * @author Maksym Symonov
 * @author Yanming Zhou
 */
public class NativeQueryTupleTransformerTest {

    @SuppressWarnings("deprecation")
    private final NativeQueryTupleTransformer nativeQueryTupleTransformer = new NativeQueryTupleTransformer();

    private final NativeQueryTupleTransformer nativeQueryTupleTransformerWithNamingStrategy = new NativeQueryTupleTransformer(new CamelCaseToUnderscoresNamingStrategy(), null);

    @Test
    public void nullValueIsExtractedFromTuple() {
        final Tuple tuple = nativeQueryTupleTransformer.transformTuple(
            new Object[] { 1L, null },
            new String[] { "id", "value" }
        );
        assertEquals(1L, tuple.get("id"));
        assertNull(tuple.get("value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingAliasCausesExceptionWhenIsExtractedFromTuple() {
        final Tuple tuple = nativeQueryTupleTransformer.transformTuple(
            new Object[] { 1L, null },
            new String[] { "id", "value" }
        );
        tuple.get("unknownAlias");
    }

    @Test
    public void camelCaseIsExtractedFromUnderscoresTuple() {
        final Tuple tuple = nativeQueryTupleTransformerWithNamingStrategy.transformTuple(
                new Object[] { "Michael Jordan" },
                new String[] { "full_name" }
        );
        assertEquals("Michael Jordan", tuple.get("fullName"));
        assertEquals("Michael Jordan", tuple.get("full_name"));
        assertEquals("Michael Jordan", tuple.get("FULL_NAME"));
    }

}
