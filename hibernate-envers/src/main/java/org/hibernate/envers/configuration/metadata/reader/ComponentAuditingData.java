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
 *
 */
package org.hibernate.envers.configuration.metadata.reader;

import static org.hibernate.envers.tools.Tools.*;

import java.util.Map;

/**
 * Audit mapping meta-data for component.
 * @author Adam Warski (adam at warski dot org)
 */
public class ComponentAuditingData extends PropertyAuditingData implements AuditedPropertiesHolder {
	private final Map<String, PropertyAuditingData> properties;

	public ComponentAuditingData() {
		this.properties = newHashMap();
	}

	public void addPropertyAuditingData(String propertyName, PropertyAuditingData auditingData) {
		properties.put(propertyName, auditingData);
	}

    public PropertyAuditingData getPropertyAuditingData(String propertyName) {
        return properties.get(propertyName);
    }
}