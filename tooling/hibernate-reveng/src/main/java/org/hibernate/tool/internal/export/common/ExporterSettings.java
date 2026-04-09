/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.export.common;

public interface ExporterSettings {

	public final String PREFIX_KEY = "hibernatetool.";
	
	/** 
	 * if true exporters are allowed to generate EJB3 constructs
	 */
	public final String EJB3 = PREFIX_KEY + "ejb3";
	
	/** 
	 * if true then exporters are allowed to generate JDK 5 constructs
	 */
	public final String JDK5 = PREFIX_KEY + "jdk5";
	
	/** 
	 * the (root) output directory for an exporter
	 */
	public final String OUTPUT_DIRECTORY = PREFIX_KEY + "output_directory";
	
	/** 
	 * the (root) output directory for an exporter
	 */
	public final String TEMPLATE_PATH = PREFIX_KEY + "template_path";
	
	
	
}
