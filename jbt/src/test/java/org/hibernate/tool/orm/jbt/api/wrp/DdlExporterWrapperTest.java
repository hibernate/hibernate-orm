/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.orm.jbt.api.wrp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Properties;

import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.internal.export.common.AbstractExporter;
import org.hibernate.tool.reveng.internal.export.ddl.DdlExporter;
import org.hibernate.tool.orm.jbt.internal.factory.DdlExporterWrapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DdlExporterWrapperTest {

	private DdlExporterWrapper ddlExporterWrapper = null; 
	private DdlExporter wrappedDdlExporter = null;
	
	@BeforeEach
	public void beforeEach() {
		wrappedDdlExporter = new DdlExporter();
		ddlExporterWrapper = DdlExporterWrapperFactory.createDdlExporterWrapper(wrappedDdlExporter);
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(ddlExporterWrapper);
		assertNotNull(wrappedDdlExporter);
	}
	
	@Test
	public void testSetExport() {
		assertNull(wrappedDdlExporter.getProperties().get(ExporterConstants.EXPORT_TO_DATABASE));
		ddlExporterWrapper.setExport(false);
		assertFalse((Boolean)wrappedDdlExporter.getProperties().get(ExporterConstants.EXPORT_TO_DATABASE));
		ddlExporterWrapper.setExport(true);
		assertTrue((Boolean)wrappedDdlExporter.getProperties().get(ExporterConstants.EXPORT_TO_DATABASE));
	}

	@Test
	public void testGetProperties() throws Exception {
		Field propertiesField = AbstractExporter.class.getDeclaredField("properties");
		propertiesField.setAccessible(true);
		Properties properties = new Properties();
		assertNotSame(properties, ddlExporterWrapper.getProperties());
		propertiesField.set(wrappedDdlExporter, properties);
		assertSame(properties, ddlExporterWrapper.getProperties());
	}

}
