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

/**
 * Holds information necessary to create an audit table: its name, schema and catalog, as well as the audit
 * entity name.
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditTableData {
    private final String auditEntityName;
    private final String auditTableName;
    private final String schema;
    private final String catalog;

    public AuditTableData(String auditEntityName, String auditTableName, String schema, String catalog) {
        this.auditEntityName = auditEntityName;
        this.auditTableName = auditTableName;
        this.schema = schema;
        this.catalog = catalog;
    }

    public String getAuditEntityName() {
        return auditEntityName;
    }

    public String getAuditTableName() {
        return auditTableName;
    }

    public String getSchema() {
        return schema;
    }

    public String getCatalog() {
        return catalog;
    }
}
