/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Nicolas Doroskevich
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class GlobalConfiguration {
	private final EnversService enversService;

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
	private boolean trackEntitiesChangedInRevision;

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
	
	// While deleting revision entry, remove data of associated audited entities
	private final boolean cascadeDeleteRevision;

	// Support reused identifiers of previously deleted entities
	private final boolean allowIdentifierReuse;

	/*
		 Which operator to use in correlated subqueries (when we want a property to be equal to the result of
		 a correlated subquery, for example: e.p <operator> (select max(e2.p) where e2.p2 = e.p2 ...).
		 Normally, this should be "=". However, HSQLDB has an issue related to that, so as a workaround,
		 "in" is used. See {@link org.hibernate.envers.test.various.HsqlTest}.
	*/
	private final String correlatedSubqueryOperator;

	public GlobalConfiguration(
			EnversService enversService,
			Map properties) {
		this.enversService = enversService;

		generateRevisionsForCollections = ConfigurationHelper.getBoolean(
				EnversSettings.REVISION_ON_COLLECTION_CHANGE,
				properties,
				true
		);

		doNotAuditOptimisticLockingField = ConfigurationHelper.getBoolean(
				EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD,
				properties,
				true
		);

		storeDataAtDelete = ConfigurationHelper.getBoolean(
				EnversSettings.STORE_DATA_AT_DELETE,
				properties,
				false
		);

		defaultSchemaName = (String) properties.get( EnversSettings.DEFAULT_SCHEMA );
		defaultCatalogName = (String) properties.get( EnversSettings.DEFAULT_CATALOG );

		correlatedSubqueryOperator = HSQLDialect.class.getName().equals( properties.get( Environment.DIALECT ) )
				? "in"
				: "=";

		trackEntitiesChangedInRevision = ConfigurationHelper.getBoolean(
				EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION,
				properties,
				false
		);
		
		cascadeDeleteRevision = ConfigurationHelper.getBoolean(
				"org.hibernate.envers.cascade_delete_revision",
				properties,
				false
		);

		useRevisionEntityWithNativeId = ConfigurationHelper.getBoolean(
				EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID,
				properties,
				true
		);

		hasGlobalSettingForWithModifiedFlag = properties.get( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG ) != null;
		globalWithModifiedFlag = ConfigurationHelper.getBoolean(
				EnversSettings.GLOBAL_WITH_MODIFIED_FLAG,
				properties,
				false
		);
		modifiedFlagSuffix = ConfigurationHelper.getString(
				EnversSettings.MODIFIED_FLAG_SUFFIX,
				properties,
				"_MOD"
		);

		final String revisionListenerClassName = (String) properties.get( EnversSettings.REVISION_LISTENER );
		if ( revisionListenerClassName != null ) {
			try {
				revisionListenerClass = ReflectionTools.loadClass(
						revisionListenerClassName,
						enversService.getClassLoaderService()
				);
			}
			catch (ClassLoadingException e) {
				throw new MappingException(
						"Revision listener class not found: " + revisionListenerClassName + ".",
						e
				);
			}
		}
		else {
			revisionListenerClass = null;
		}

		allowIdentifierReuse = ConfigurationHelper.getBoolean(
				EnversSettings.ALLOW_IDENTIFIER_REUSE, properties, false
		);
	}

	public EnversService getEnversService() {
		return enversService;
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

	public boolean isTrackEntitiesChangedInRevision() {
		return trackEntitiesChangedInRevision;
	}

	public void setTrackEntitiesChangedInRevision(boolean trackEntitiesChangedInRevision) {
		this.trackEntitiesChangedInRevision = trackEntitiesChangedInRevision;
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
	
	public boolean isCascadeDeleteRevision() {
		return cascadeDeleteRevision;
	}

	public boolean isAllowIdentifierReuse() {
		return allowIdentifierReuse;
	}
}
