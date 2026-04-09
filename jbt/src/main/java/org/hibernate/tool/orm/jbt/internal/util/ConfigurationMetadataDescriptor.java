/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2023-2025 Red Hat, Inc.
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
package org.hibernate.tool.orm.jbt.internal.util;

import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;

public class ConfigurationMetadataDescriptor implements MetadataDescriptor {
	
	private Configuration configuration = null;
	
	public ConfigurationMetadataDescriptor(Configuration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public Metadata createMetadata() {
		return MetadataHelper.getMetadata(configuration);
	}

	@Override
	public Properties getProperties() {
		return configuration.getProperties();
	}

}
