package org.hibernate.tool.api.export;

import org.junit.Assert;
import org.junit.Test;


public class ExporterTypeTest {
	
	@Test
	public void testExporterType() {
		Assert.assertEquals(
				"org.hibernate.tool.internal.export.pojo.POJOExporter",
				ExporterType.POJO.className());
	}

}
