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
package org.hibernate.envers.configuration.internal;


import org.hibernate.MappingException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.configuration.EnversSettings;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Nicolas Doroskevich
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
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

	public GlobalConfiguration(StandardServiceRegistry serviceRegistry) {

		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );

		generateRevisionsForCollections = configurationService.getSetting(
				EnversSettings.REVISION_ON_COLLECTION_CHANGE, StandardConverters.BOOLEAN, true
		);

		doNotAuditOptimisticLockingField = configurationService.getSetting(
				EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD, StandardConverters.BOOLEAN, true
		);

		storeDataAtDelete = configurationService.getSetting(
				EnversSettings.STORE_DATA_AT_DELETE, StandardConverters.BOOLEAN, false
		);

		defaultSchemaName = configurationService.getSetting(
				EnversSettings.DEFAULT_SCHEMA, StandardConverters.STRING
		);
		defaultCatalogName = configurationService.getSetting(
				EnversSettings.DEFAULT_CATALOG, StandardConverters.STRING
		);

		// TODO: is this really needed??? Should be available in dialect...
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		correlatedSubqueryOperator = HSQLDialect.class.equals( jdbcEnvironment.getDialect().getClass() ) ? "in" : "=";

		trackEntitiesChangedInRevision = configurationService.getSetting(
				EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, StandardConverters.BOOLEAN, false
		);

		// TODO: shouldn't there be an Envers setting for "org.hibernate.envers.cascade_delete_revision"?
		cascadeDeleteRevision = configurationService.getSetting(
				"org.hibernate.envers.cascade_delete_revision", StandardConverters.BOOLEAN, false
		);

		useRevisionEntityWithNativeId = configurationService.getSetting(
				EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, StandardConverters.BOOLEAN, true
		);

		hasGlobalSettingForWithModifiedFlag = null != configurationService.getSetting(
				EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, StandardConverters.BOOLEAN
		);
		globalWithModifiedFlag = configurationService.getSetting(
				EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, StandardConverters.BOOLEAN, false
		);
		modifiedFlagSuffix = configurationService.getSetting(
				EnversSettings.MODIFIED_FLAG_SUFFIX, StandardConverters.STRING, "_MOD"
		);

		final String revisionListenerClassName = configurationService.getSetting(
				EnversSettings.REVISION_LISTENER,
				StandardConverters.STRING
		);
		if ( revisionListenerClassName != null ) {
			try {
				revisionListenerClass =
						serviceRegistry.getService( ClassLoaderService.class ).classForName( revisionListenerClassName );
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

		allowIdentifierReuse = configurationService.getSetting(
				EnversSettings.ALLOW_IDENTIFIER_REUSE, StandardConverters.BOOLEAN, false
		);
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
