/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2017-2025 Red Hat, Inc.
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
package org.hibernate.tool.api.metadata;

import java.io.File;
import java.util.Properties;

import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.metadata.JpaMetadataDescriptor;
import org.hibernate.tool.internal.metadata.NativeMetadataDescriptor;

public class MetadataDescriptorFactory {
	
	public static MetadataDescriptor createReverseEngineeringDescriptor(
			RevengStrategy reverseEngineeringStrategy, 
			Properties properties) {
		return new RevengMetadataDescriptor(
				reverseEngineeringStrategy, 
				properties);
	}
	
	public static MetadataDescriptor createJpaDescriptor(String persistenceUnit, Properties properties) {
		return new JpaMetadataDescriptor(persistenceUnit, properties);
	}
	
	public static MetadataDescriptor createNativeDescriptor(
			File cfgXmlFile,
			File[] mappingFiles,
			Properties properties) {
		return new NativeMetadataDescriptor(
				cfgXmlFile, 
				mappingFiles, 
				properties);
	}
	
}
