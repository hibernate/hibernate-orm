/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.hbm;

import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.tool.reveng.internal.export.common.EntityNameFromValueVisitor;

import java.util.Objects;
import java.util.Properties;

public class Cfg2HbmTool {

	public static Properties getFilteredIdentifierGeneratorProperties(Properties properties, Properties environmentProperties) {
		if (properties != null){
			Properties fProp = new Properties();
			for (Object o : properties.keySet()) {
				String key = (String) o;
				if ("schema".equals(key)) {
					String schema = properties.getProperty(key);
					if (!isDefaultSchema(schema, environmentProperties)) {
						fProp.put(key, schema);
					}
				}
				else if ("catalog".equals(key)) {
					String catalog = properties.getProperty(key);
					if (!isDefaultCatalog(catalog, environmentProperties)) {
						fProp.put(key, catalog);
					}
				}
				else if (!key.startsWith("target_")) {
					fProp.put(key, properties.get(key));
				}
			}
			return fProp;
		}
		return null;
	}

	static private boolean isDefaultSchema(String schema, Properties properties) {
		String defaultSchema = properties.getProperty(Environment.DEFAULT_SCHEMA);
		return Objects.equals( defaultSchema, schema );
	}

	static private boolean isDefaultCatalog(String catalog, Properties properties) {
		String defaultCatalog = properties.getProperty(Environment.DEFAULT_CATALOG);
		return Objects.equals( defaultCatalog, catalog );
	}

	public boolean isOneToOne(Property property) {
		return (property.getValue() instanceof OneToOne);
	}

	public boolean isManyToOne(Property property) {
		return isManyToOne(property.getValue());
	}

	public boolean isManyToOne(Value value) {
		return (value instanceof ManyToOne);
	}

	public boolean isCollection(Property property) {
		return property.getValue() instanceof Collection;
	}

	public String getHibernateTypeName(Property p) {
		return (String) p.getValue().accept(new EntityNameFromValueVisitor());
	}

	public String getSafeHibernateTypeName(Property p) {
		return (String) p.getValue().accept(new EntityNameFromValueVisitor(false));
	}
}
