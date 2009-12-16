/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.configuration.metadata;

import java.util.Iterator;
import java.util.Properties;

import org.dom4j.Element;
import org.hibernate.envers.entities.mapper.SimpleMapperBuilder;
import org.hibernate.envers.configuration.metadata.reader.PropertyAuditingData;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.CustomType;
import org.hibernate.type.ImmutableType;
import org.hibernate.type.MutableType;
import org.hibernate.type.Type;

/**
 * Generates metadata for basic properties: immutable types (including enums).
 * @author Adam Warski (adam at warski dot org)
 */
public final class BasicMetadataGenerator {
	boolean addBasic(Element parent, PropertyAuditingData propertyAuditingData,
					 Value value, SimpleMapperBuilder mapper, boolean insertable, boolean key) {
		Type type = value.getType();

		if (type instanceof ImmutableType || type instanceof MutableType) {
			addSimpleValue(parent, propertyAuditingData, value, mapper, insertable, key);
		} else if (type instanceof CustomType || type instanceof CompositeCustomType) {
			addCustomValue(parent, propertyAuditingData, value, mapper, insertable, key);
		} else if ("org.hibernate.type.PrimitiveByteArrayBlobType".equals(type.getClass().getName())) {
			addSimpleValue(parent, propertyAuditingData, value, mapper, insertable, key);
		} else {
			return false;
		}

		return true;
	}

	@SuppressWarnings({"unchecked"})
	private void addSimpleValue(Element parent, PropertyAuditingData propertyAuditingData,
								Value value, SimpleMapperBuilder mapper, boolean insertable, boolean key) {
		if (parent != null) {
			Element prop_mapping = MetadataTools.addProperty(parent, propertyAuditingData.getName(),
					value.getType().getName(), propertyAuditingData.isForceInsertable() || insertable, key);
			MetadataTools.addColumns(prop_mapping, (Iterator<Column>) value.getColumnIterator());
		}

		// A null mapper means that we only want to add xml mappings
		if (mapper != null) {
			mapper.add(propertyAuditingData.getPropertyData());
		}
	}

	@SuppressWarnings({"unchecked"})
	private void addCustomValue(Element parent, PropertyAuditingData propertyAuditingData,
								Value value, SimpleMapperBuilder mapper, boolean insertable, boolean key) {
		if (parent != null) {
			Element prop_mapping = MetadataTools.addProperty(parent, propertyAuditingData.getName(),
					null, insertable, key);

			//CustomType propertyType = (CustomType) value.getType();

			Element type_mapping = prop_mapping.addElement("type");
			type_mapping.addAttribute("name", value.getType().getName());

			if (value instanceof SimpleValue) {
				Properties typeParameters = ((SimpleValue) value).getTypeParameters();
				if (typeParameters != null) {
					for (java.util.Map.Entry paramKeyValue : typeParameters.entrySet()) {
						Element type_param = type_mapping.addElement("param");
						type_param.addAttribute("name", (String) paramKeyValue.getKey());
						type_param.setText((String) paramKeyValue.getValue());
					}
				}
			}

			MetadataTools.addColumns(prop_mapping, (Iterator<Column>) value.getColumnIterator());
		}

		if (mapper != null) {
			mapper.add(propertyAuditingData.getPropertyData());
		}
	}
}
