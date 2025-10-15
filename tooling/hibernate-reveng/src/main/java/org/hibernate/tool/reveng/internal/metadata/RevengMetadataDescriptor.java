/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		}
		else {
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
