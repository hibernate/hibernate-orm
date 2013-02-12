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

import org.hibernate.MappingException;
import org.hibernate.envers.RevisionListener;
import org.hibernate.internal.util.ReflectHelper;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Nicolas Doroskevich
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class GlobalConfiguration {
	public static final String GLOBAL_WITH_MODIFIED_FLAG_PROPERTY = "org.hibernate.envers.global_with_modified_flag";
	public static final String MODIFIED_FLAG_SUFFIX_PROPERTY = "org.hibernate.envers.modified_flag_suffix";
	public static final String DEFAULT_MODIFIED_FLAG_SUFFIX = "_MOD";

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

    // Should Envers track (persist) entity names that have been changed during each revision.
    private boolean trackEntitiesChangedInRevisionEnabled;

    // Revision listener class name.
    private final Class<? extends RevisionListener> revisionListenerClass;

	// Should Envers use modified property flags by default
    private boolean globalWithModifiedFlag;

	// Indicates that user defined global behavior for modified flags feature
	private boolean hasGlobalSettingForWithModifiedFlag;

	// Suffix to be used for modified flags columns
	private String modifiedFlagSuffix;

    // Use revision entity with native id generator
    private final boolean useRevisionEntityWithNativeId;

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

        String trackEntitiesChangedInRevisionEnabledStr = getProperty(properties,
        		"org.hibernate.envers.track_entities_changed_in_revision",
        		"org.hibernate.envers.track_entities_changed_in_revision",
        		"false");
        trackEntitiesChangedInRevisionEnabled = Boolean.parseBoolean(trackEntitiesChangedInRevisionEnabledStr);

        String useRevisionEntityWithNativeIdStr = getProperty(properties,
        		"org.hibernate.envers.use_revision_entity_with_native_id",
        		"org.hibernate.envers.use_revision_entity_with_native_id",
        		"true");
        useRevisionEntityWithNativeId = Boolean.parseBoolean(useRevisionEntityWithNativeIdStr);

		hasGlobalSettingForWithModifiedFlag =
				properties.getProperty(GLOBAL_WITH_MODIFIED_FLAG_PROPERTY) != null;
		String usingModifiedFlagStr = getProperty(properties,
                GLOBAL_WITH_MODIFIED_FLAG_PROPERTY,
                GLOBAL_WITH_MODIFIED_FLAG_PROPERTY,
        		"false");
        globalWithModifiedFlag = Boolean.parseBoolean(usingModifiedFlagStr);

		modifiedFlagSuffix =
				getProperty(properties, MODIFIED_FLAG_SUFFIX_PROPERTY,
						MODIFIED_FLAG_SUFFIX_PROPERTY,
						DEFAULT_MODIFIED_FLAG_SUFFIX);

		String revisionListenerClassName = properties.getProperty("org.hibernate.envers.revision_listener", null);
        if (revisionListenerClassName != null) {
            try {
                revisionListenerClass = (Class<? extends RevisionListener>) ReflectHelper.classForName(revisionListenerClassName);
            } catch (ClassNotFoundException e) {
                throw new MappingException("Revision listener class not found: " + revisionListenerClassName + ".", e);
            }
        } else {
            revisionListenerClass = null;
        }
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

    public boolean isTrackEntitiesChangedInRevisionEnabled() {
        return trackEntitiesChangedInRevisionEnabled;
    }

    public void setTrackEntitiesChangedInRevisionEnabled(boolean trackEntitiesChangedInRevisionEnabled) {
        this.trackEntitiesChangedInRevisionEnabled = trackEntitiesChangedInRevisionEnabled;
    }

    public Class<? extends RevisionListener> getRevisionListenerClass() {
        return revisionListenerClass;
    }
	
	public boolean hasSettingForUsingModifiedFlag() {
		return hasGlobalSettingForWithModifiedFlag;
	}

	public boolean isGlobalWithModifiedFlag() {
		return globalWithModifiedFlag;
	}

	public String getModifiedFlagSuffix() {
		return modifiedFlagSuffix;
	}

    public boolean isUseRevisionEntityWithNativeId() {
        return useRevisionEntityWithNativeId;
    }
}
