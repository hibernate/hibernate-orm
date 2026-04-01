/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.test.utils;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

public class HibernateUtil {

	public static class Dialect extends org.hibernate.dialect.Dialect {
		public Dialect() {
			super((DatabaseVersion)null);
		}
	}

	public static ForeignKey getForeignKey(Table table, String fkName) {
		ForeignKey result = null;
		for (ForeignKey fk : table.getForeignKeyCollection()) {
			if (fk.getName().equals(fkName)) {
				result = fk;
				break;
			}
		}
		return result;
	}

	public static Table getTable(Metadata metadata, String tabName) {
		if (metadata != null) {
			for (Table table : metadata.collectTableMappings()) {
				if (table.getName().equals(tabName)) {
					return table;
				}
			}
		}
		return null;
	}

	public static MetadataDescriptor initializeMetadataDescriptor(
			Object test,
			String[] hbmResourceNames,
			File hbmFileDir) {
		ResourceUtil.createResources(test, hbmResourceNames, hbmFileDir);
		File[] hbmFiles = new File[hbmResourceNames.length];
		for (int i = 0; i < hbmResourceNames.length; i++) {
			hbmFiles[i] = new File(hbmFileDir, hbmResourceNames[i]);
		}
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, Dialect.class.getName());
		properties.setProperty(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
		return MetadataDescriptorFactory.createNativeDescriptor(null, hbmFiles, properties);
	}

	public static void addAnnotatedClass(
			MetadataDescriptor metadataDescriptor,
			Class<?> annotatedClass) {
		try {
			Field metadataSourcesField = metadataDescriptor
					.getClass()
					.getDeclaredField("metadataSources");
			metadataSourcesField.setAccessible(true);
			MetadataSources metadataSources =
					(MetadataSources)metadataSourcesField.get(metadataDescriptor);
			metadataSources.addAnnotatedClass(annotatedClass);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
