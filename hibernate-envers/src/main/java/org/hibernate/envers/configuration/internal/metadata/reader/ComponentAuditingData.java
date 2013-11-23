/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.Map;
import java.util.Set;

import static org.hibernate.envers.internal.tools.Tools.newHashMap;

/**
 * Audit mapping meta-data for component.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 */
public class ComponentAuditingData extends PropertyAuditingData implements AuditedPropertiesHolder {
	private final Map<String, PropertyAuditingData> properties;

	public ComponentAuditingData() {
		this.properties = newHashMap();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public void addPropertyAuditingData(String propertyName, PropertyAuditingData auditingData) {
		properties.put( propertyName, auditingData );
	}

	@Override
	public PropertyAuditingData getPropertyAuditingData(String propertyName) {
		return properties.get( propertyName );
	}

	@Override
	public boolean contains(String propertyName) {
		return properties.containsKey( propertyName );
	}

	public Set<String> getPropertyNames() {
		return properties.keySet();
	}
}
