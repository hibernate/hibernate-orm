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
package org.hibernate.tool.gradle;

import org.gradle.api.Project;

public class Extension {
	
	public String sqlToRun = "";
	public String hibernateProperties = "hibernate.properties";
	public String outputFolder = "generated-sources";
	public String packageName = "";
	public String revengStrategy = null;
	public Boolean generateAnnotations = true;
	
	public Extension(Project project) {}
	
}
