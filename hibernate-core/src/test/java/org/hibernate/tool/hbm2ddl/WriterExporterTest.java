package org.hibernate.tool.hbm2ddl;

import org.hibernate.dialect.Mocks;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WriterExporterTest {

    @Test
    public void testExportContent() throws Exception {
        Writer out = new StringWriter();

        WriterExporter exporter = new WriterExporter(out, false);
        exporter.export("line1");
        exporter.export("line2");
        exporter.release();

        assertEquals("lin1\nline2\n", out.toString());
    }


    @Test
    public void testExportReleased() throws Exception {
        Writer out = Mockito.mock(Writer.class);

        WriterExporter exporter = new WriterExporter(out, false);
        exporter.export("line1");
        exporter.release();

        Mockito.verify(out, atLeastOnce()).close();
    }
}