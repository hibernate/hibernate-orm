package org.hibernate.tool.api.export;

import org.hibernate.tool.internal.export.common.AbstractExporter;
import org.junit.Assert;
import org.junit.Test;

public class ExporterFactoryTest {
	
	@Test
	public void testCreateExporter() {
		try {
			ExporterFactory.createExporter("foobar");
			Assert.fail();
		} catch(Throwable t) {
			Assert.assertTrue(t.getMessage().contains("foobar"));
		}
		Exporter exporter = ExporterFactory.createExporter(
				"org.hibernate.tool.api.export.ExporterFactoryTest$TestExporter");
		Assert.assertNotNull(exporter);
		Assert.assertTrue(exporter instanceof TestExporter);
		exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		Assert.assertNotNull(exporter);
		Assert.assertEquals(
				ExporterType.JAVA.className(), 
				exporter.getClass().getName());
	}
	
	public static class TestExporter extends AbstractExporter {
		@Override protected void doStart() {}
	}

}
