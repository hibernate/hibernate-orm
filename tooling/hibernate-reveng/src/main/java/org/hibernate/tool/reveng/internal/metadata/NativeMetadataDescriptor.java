/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.metadata;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;

import java.io.File;
import java.util.Properties;

public class NativeMetadataDescriptor implements MetadataDescriptor {

	private final Properties properties = new Properties();
	private final MappingSources mappingSources = new MappingSources();
	private final StandardServiceRegistryBuilder ssrb;
	private static final String CFG_XML_UNSUPPORTED_MESSAGE =
			"Legacy hibernate.cfg.xml bootstrap is no longer supported; use properties and mapping files";

	public NativeMetadataDescriptor(
			File cfgXmlFile,
			File[] mappingFiles,
			Properties properties) {
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		ssrb = new StandardServiceRegistryBuilder(bsr);
		if (cfgXmlFile != null) {
			throw new UnsupportedOperationException( CFG_XML_UNSUPPORTED_MESSAGE );
		}
		if (properties != null) {
			this.properties.putAll(properties);
		}
		ssrb.applySettings(getProperties());
		if (mappingFiles != null) {
			for (File file : mappingFiles) {
				if (file.getName().endsWith(".jar")) {
					throw new IllegalArgumentException(
							"Jar file mapping discovery is no longer supported: " + file
					);
				}
				else {
					mappingSources.addMappingFile(file);
				}
			}
		}
	}

	public Properties getProperties() {
		Properties result = new Properties();
		result.putAll(properties);
		return result;
	}

	public void addAnnotatedClass(Class<?> annotatedClass) {
		mappingSources.addManagedClass(annotatedClass);
	}

	public Metadata createMetadata() {
		final StandardServiceRegistry serviceRegistry = ssrb.build();
		return MetadataBuildingHelper.buildMetadata(serviceRegistry, mappingSources);
	}

}
