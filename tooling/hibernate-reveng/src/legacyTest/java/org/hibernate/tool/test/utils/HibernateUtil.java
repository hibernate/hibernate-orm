/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
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
package org.hibernate.tool.test.utils;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;

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
