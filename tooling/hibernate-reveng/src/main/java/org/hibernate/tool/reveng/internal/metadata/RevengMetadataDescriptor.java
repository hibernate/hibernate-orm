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

import java.util.Objects;
import java.util.Properties;

public class RevengMetadataDescriptor implements MetadataDescriptor {

	private final RevengStrategy reverseEngineeringStrategy;
	private final Properties properties = new Properties();

	public RevengMetadataDescriptor(
			RevengStrategy reverseEngineeringStrategy,
			Properties properties) {
		this.properties.putAll(Environment.getProperties());
		if (properties != null) {
			this.properties.putAll(properties);
		}
		this.reverseEngineeringStrategy = Objects.requireNonNullElseGet( reverseEngineeringStrategy,
				RevengStrategyFactory::createReverseEngineeringStrategy );
		this.properties.putIfAbsent( MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, true );
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
