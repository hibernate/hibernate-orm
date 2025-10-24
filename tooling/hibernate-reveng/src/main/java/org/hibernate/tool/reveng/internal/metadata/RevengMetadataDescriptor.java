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
package org.hibernate.tool.reveng.internal.metadata;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.reveng.api.metadata.MetadataConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.RevengStrategyFactory;
import org.hibernate.tool.reveng.internal.core.RevengMetadataBuilder;

import java.util.Properties;

public class RevengMetadataDescriptor implements MetadataDescriptor {
	
	private RevengStrategy reverseEngineeringStrategy = null;
    private Properties properties = new Properties();

	public RevengMetadataDescriptor(
			RevengStrategy reverseEngineeringStrategy, 
			Properties properties) {
		this.properties.putAll(Environment.getProperties());
		if (properties != null) {
			this.properties.putAll(properties);
		}
		if (reverseEngineeringStrategy != null) {
			this.reverseEngineeringStrategy = reverseEngineeringStrategy;
		} else {
			this.reverseEngineeringStrategy = RevengStrategyFactory.createReverseEngineeringStrategy();
		}
		if (this.properties.get(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS) == null) {
			this.properties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, true);
		}
	}

	public Properties getProperties() {
		Properties result = new Properties();
		result.putAll(properties);
		return result;
	}
    
	public Metadata createMetadata() {
		return RevengMetadataBuilder
				.create(properties, reverseEngineeringStrategy)
				.build();
	}
	
}
