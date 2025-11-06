/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.metadata;

import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.internal.metadata.JpaMetadataDescriptor;
import org.hibernate.tool.reveng.internal.metadata.NativeMetadataDescriptor;
import org.hibernate.tool.reveng.internal.metadata.RevengMetadataDescriptor;

import java.io.File;
import java.util.Properties;

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
