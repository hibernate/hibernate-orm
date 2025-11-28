package org.hibernate.tool.internal.export.mapping;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class MappingExporterTest {

    @Test
    public void testStart() {
        MappingExporter exporter = new MappingExporter();
        assertNotNull(exporter);
        exporter.start();
    }

}
