package org.hibernate.tool.maven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DummyDialectTest {

    @Test
    public void testDummyDialect() {
        assertNotNull(DummyDialect.INSTANCE);
    }

}
