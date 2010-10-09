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

import java.util.Properties;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Nicolas Doroskevich
 */
public class GlobalConfiguration {
    // Should a revision be generated when a not-owned relation field changes
    private final boolean generateRevisionsForCollections;

    // Should the optimistic locking property of an entity be considered unversioned
    private final boolean doNotAuditOptimisticLockingField;

	// Should entity data be stored when it is deleted
	private final boolean storeDataAtDelete;

    // The default name of the schema of audit tables.
    private final String defaultSchemaName;

    // The default name of the catalog of the audit tables.
    private final String defaultCatalogName;

    /*
     Which operator to use in correlated subqueries (when we want a property to be equal to the result of
     a correlated subquery, for example: e.p <operator> (select max(e2.p) where e2.p2 = e.p2 ...).
     Normally, this should be "=". However, HSQLDB has an issue related to that, so as a workaround,
     "in" is used. See {@link org.hibernate.envers.test.various.HsqlTest}.
     */
    private final String correlatedSubqueryOperator;

    public GlobalConfiguration(Properties properties) {
        String generateRevisionsForCollectionsStr = getProperty(properties,
                "org.hibernate.envers.revision_on_collection_change",
                "org.hibernate.envers.revisionOnCollectionChange",
                "true");
        generateRevisionsForCollections = Boolean.parseBoolean(generateRevisionsForCollectionsStr);

        String ignoreOptimisticLockingPropertyStr = getProperty(properties,
                "org.hibernate.envers.do_not_audit_optimistic_locking_field",
                "org.hibernate.envers.doNotAuditOptimisticLockingField",
                "true");
        doNotAuditOptimisticLockingField = Boolean.parseBoolean(ignoreOptimisticLockingPropertyStr);

		String storeDataDeletedEntityStr = getProperty(properties,
                "org.hibernate.envers.store_data_at_delete",
                "org.hibernate.envers.storeDataAtDelete",
                "false");
		storeDataAtDelete = Boolean.parseBoolean(storeDataDeletedEntityStr);

        defaultSchemaName = properties.getProperty("org.hibernate.envers.default_schema", null);
        defaultCatalogName = properties.getProperty("org.hibernate.envers.default_catalog", null);

        correlatedSubqueryOperator = "org.hibernate.dialect.HSQLDialect".equals(
                properties.getProperty("hibernate.dialect")) ? "in" : "=";
    }

    public boolean isGenerateRevisionsForCollections() {
        return generateRevisionsForCollections;
    }

    public boolean isDoNotAuditOptimisticLockingField() {
        return doNotAuditOptimisticLockingField;
    }

    public String getCorrelatedSubqueryOperator() {
        return correlatedSubqueryOperator;
    }

	public boolean isStoreDataAtDelete() {
		return storeDataAtDelete;
	}

    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    public String getDefaultCatalogName() {
        return defaultCatalogName;
    }
}
