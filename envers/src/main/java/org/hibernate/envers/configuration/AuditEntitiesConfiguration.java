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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration of versions entities - names of fields, entities and tables created to store versioning information.
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditEntitiesConfiguration {
    private final String versionsTablePrefix;
    private final String versionsTableSuffix;

    private final String originalIdPropName;

    private final String revisionPropName;
    private final String revisionPropPath;

    private final String revisionTypePropName;
    private final String revisionTypePropType;

    private final String revisionInfoEntityName;

    private final Map<String, String> customVersionsTablesNames;

    public AuditEntitiesConfiguration(Properties properties, String revisionInfoEntityName) {
        this.revisionInfoEntityName = revisionInfoEntityName;

        versionsTablePrefix = properties.getProperty("org.hibernate.envers.auditTablePrefix", "");
        versionsTableSuffix = properties.getProperty("org.hibernate.envers.auditTableSuffix", "_AUD");

        originalIdPropName = "originalId";

        revisionPropName = properties.getProperty("org.hibernate.envers.revisionFieldName", "REV");

        revisionTypePropName = properties.getProperty("org.hibernate.envers.revisionTypeFieldName", "REVTYPE");
        revisionTypePropType = "byte";

        customVersionsTablesNames = new HashMap<String, String>();

        revisionPropPath = originalIdPropName + "." + revisionPropName + ".id";
    }

    public String getOriginalIdPropName() {
        return originalIdPropName;
    }

    public String getRevisionPropName() {
        return revisionPropName;
    }

    public String getRevisionPropPath() {
        return revisionPropPath;
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

    public void addCustomVersionsTableName(String entityName, String tableName) {
        customVersionsTablesNames.put(entityName, tableName);
    }

    //

    public String getVersionsEntityName(String entityName) {
        return versionsTablePrefix + entityName + versionsTableSuffix;
    }

    public String getVersionsTableName(String entityName, String tableName) {
        String customHistoryTableName = customVersionsTablesNames.get(entityName);
        if (customHistoryTableName == null) {
            return versionsTablePrefix + tableName + versionsTableSuffix;
        }

        return customHistoryTableName;
    }
}
