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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.internal.export.common.GenericExporter;
import org.hibernate.tool.orm.jbt.internal.factory.GenericExporterWrapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GenericExporterWrapperTest {

	private GenericExporterWrapper genericExporterWrapper = null; 
	private GenericExporter wrappedGenericExporter = null;
	
	@BeforeEach
	public void beforeEach() {
		wrappedGenericExporter = new GenericExporter();
		genericExporterWrapper = GenericExporterWrapperFactory.createGenericExporterWrapper(wrappedGenericExporter);
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(wrappedGenericExporter);
		assertNotNull(genericExporterWrapper);
	}
	
	@Test
	public void testSetFilePattern() {
		assertNull(wrappedGenericExporter.getProperties().get(ExporterConstants.FILE_PATTERN));
		genericExporterWrapper.setFilePattern("foobar");
		assertEquals("foobar", wrappedGenericExporter.getProperties().get(ExporterConstants.FILE_PATTERN));
	}
	
	@Test
	public void testSetTemplate() {
		assertNull(wrappedGenericExporter.getProperties().get(ExporterConstants.TEMPLATE_NAME));
		genericExporterWrapper.setTemplateName("barfoo");
		assertEquals("barfoo", wrappedGenericExporter.getProperties().get(ExporterConstants.TEMPLATE_NAME));
	}
	
	@Test
	public void testSetForEach() {
		assertNull(wrappedGenericExporter.getProperties().get(ExporterConstants.FOR_EACH));
		genericExporterWrapper.setForEach("foobar");
		assertEquals("foobar", wrappedGenericExporter.getProperties().get(ExporterConstants.FOR_EACH));
	}
	
	@Test
	public void testGetFilePattern() {
		assertNull(genericExporterWrapper.getFilePattern());
		wrappedGenericExporter.getProperties().put(ExporterConstants.FILE_PATTERN, "foobar");
		assertEquals("foobar", genericExporterWrapper.getFilePattern());
	}
	
	@Test
	public void testGetTemplateName() {
		assertNull(genericExporterWrapper.getTemplateName());
		wrappedGenericExporter.getProperties().put(ExporterConstants.TEMPLATE_NAME, "foobar");
		assertEquals("foobar", genericExporterWrapper.getTemplateName());
	}
	
}
