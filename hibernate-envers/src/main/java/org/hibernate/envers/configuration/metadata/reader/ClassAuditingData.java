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
package org.hibernate.envers.configuration.metadata.reader;

import java.util.Map;

import org.hibernate.envers.AuditTable;
import static org.hibernate.envers.tools.Tools.*;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
*/
public class ClassAuditingData implements AuditedPropertiesHolder {
    private final Map<String, PropertyAuditingData> properties;
    private final Map<String, String> secondaryTableDictionary;

    private AuditTable auditTable;

	/**
	 * True if the class is audited globally (this helps to cover the cases when there are no fields in the class,
	 * but it's still audited).
	 */
	private boolean defaultAudited;

    public ClassAuditingData() {
        properties = newHashMap();
        secondaryTableDictionary = newHashMap();
    }

	public void addPropertyAuditingData(String propertyName, PropertyAuditingData auditingData) {
		properties.put(propertyName, auditingData);
	}

    public PropertyAuditingData getPropertyAuditingData(String propertyName) {
        return properties.get(propertyName);
    }

    public Iterable<String> getPropertyNames() {
        return properties.keySet();
    }

    public Map<String, String> getSecondaryTableDictionary() {
        return secondaryTableDictionary;
    }

    public AuditTable getAuditTable() {
        return auditTable;
    }

    public void setAuditTable(AuditTable auditTable) {
        this.auditTable = auditTable;
    }

	public void setDefaultAudited(boolean defaultAudited) {
		this.defaultAudited = defaultAudited;
	}

	public boolean isAudited() {
        return defaultAudited || properties.size() > 0;
    }
}
