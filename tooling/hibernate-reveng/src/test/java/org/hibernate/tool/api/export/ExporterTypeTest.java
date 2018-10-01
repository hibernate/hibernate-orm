package org.hibernate.tool.api.export;

import org.junit.Assert;
import org.junit.Test;


public class ExporterTypeTest {
	
	@Test
	public void testExporterType() {
		Assert.assertEquals(
				"org.hibernate.tool.internal.export.common.GenericExporter",
				ExporterType.GENERIC.className());
		Assert.assertEquals(
				"org.hibernate.tool.api.export.PojoExporter",
				ExporterType.POJO.className());
	}

}
