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
package org.hibernate.tool.orm.jbt.internal.factory;

import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.internal.export.common.GenericExporter;
import org.hibernate.tool.orm.jbt.api.wrp.GenericExporterWrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class GenericExporterWrapperFactory {

	public static GenericExporterWrapper createGenericExporterWrapper(final GenericExporter wrappedGenericExporter) {
		return new GenericExporterWrapperImpl(wrappedGenericExporter);
	}
	
	private static class GenericExporterWrapperImpl
			extends AbstractWrapper
			implements GenericExporterWrapper {
		
		private GenericExporter genericExporter = null;
		
		private GenericExporterWrapperImpl(GenericExporter genericExporter) {
			this.genericExporter = genericExporter;
		}
		
		@Override 
		public GenericExporter getWrappedObject() { 
			return genericExporter; 
		}
		
		@Override
		public void setFilePattern(String filePattern) { 
			genericExporter.getProperties().setProperty(
					ExporterConstants.FILE_PATTERN, filePattern);
		}
		
		@Override
		public void setTemplateName(String templateName) {
			genericExporter.getProperties().setProperty(
					ExporterConstants.TEMPLATE_NAME, templateName);
		}
		
		@Override
		public void setForEach(String forEach) {
			genericExporter.getProperties().setProperty(
					ExporterConstants.FOR_EACH, forEach);
		}
		
		@Override
		public String getFilePattern() {
			return genericExporter.getProperties().getProperty(
					ExporterConstants.FILE_PATTERN);
		}
		
		@Override
		public String getTemplateName() {
			return genericExporter.getProperties().getProperty(
					ExporterConstants.TEMPLATE_NAME);
		}

	}
	
}
