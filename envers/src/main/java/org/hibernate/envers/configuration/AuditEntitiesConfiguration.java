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
package org.hibernate.envers.configuration;

import static org.hibernate.envers.tools.Tools.getProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration of versions entities - names of fields, entities and tables created to store versioning information.
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditEntitiesConfiguration {
    private final String auditTablePrefix;
    private final String auditTableSuffix;

    private final String originalIdPropName;

    private final String revisionFieldName;
    private final String revisionNumberPath;
    private final String revisionPropBasePath;

    private final String revisionTypePropName;
    private final String revisionTypePropType;

    private final String revisionInfoEntityName;

    private final Map<String, String> customAuditTablesNames;

    public AuditEntitiesConfiguration(Properties properties, String revisionInfoEntityName) {
        this.revisionInfoEntityName = revisionInfoEntityName;

        auditTablePrefix = getProperty(properties,
                "org.hibernate.envers.audit_table_prefix",
                "org.hibernate.envers.auditTablePrefix",
                "");
        auditTableSuffix = getProperty(properties,
                "org.hibernate.envers.audit_table_suffix", 
                "org.hibernate.envers.auditTableSuffix",
                "_AUD");

        originalIdPropName = "originalId";

        revisionFieldName = getProperty(properties,
                "org.hibernate.envers.revision_field_name",
                "org.hibernate.envers.revisionFieldName",
                "REV");

        revisionTypePropName = getProperty(properties,
                "org.hibernate.envers.revision_type_field_name", 
                "org.hibernate.envers.revisionTypeFieldName",
                "REVTYPE");
        revisionTypePropType = "byte";

        customAuditTablesNames = new HashMap<String, String>();

        revisionNumberPath = originalIdPropName + "." + revisionFieldName + ".id";
        revisionPropBasePath = originalIdPropName + "." + revisionFieldName + ".";
    }

    public String getOriginalIdPropName() {
        return originalIdPropName;
    }

    public String getRevisionFieldName() {
        return revisionFieldName;
    }

    public String getRevisionNumberPath() {
        return revisionNumberPath;
    }

    /**
     * @param propertyName Property of the revision entity.
     * @return A path to the given property of the revision entity associated with an audit entity.
     */
    public String getRevisionPropPath(String propertyName) {
        return revisionPropBasePath + propertyName;
    }

    public String getRevisionTypePropName() {
        return revisionTypePropName;
    }

    public String getRevisionTypePropType() {
        return revisionTypePropType;
    }

    public String getRevisionInfoEntityName() {
        return revisionInfoEntityName;
    }

    //

    public void addCustomAuditTableName(String entityName, String tableName) {
        customAuditTablesNames.put(entityName, tableName);
    }

    //

    public String getAuditEntityName(String entityName) {
        return auditTablePrefix + entityName + auditTableSuffix;
    }

    public String getAuditTableName(String entityName, String tableName) {
        String customHistoryTableName = customAuditTablesNames.get(entityName);
        if (customHistoryTableName == null) {
            return auditTablePrefix + tableName + auditTableSuffix;
        }

        return customHistoryTableName;
    }
}
