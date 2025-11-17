/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.boot.internal.LegacyModifiedColumnNamingStrategy;
import org.hibernate.envers.boot.spi.ModifiedColumnNamingStrategy;
import org.hibernate.envers.configuration.internal.RevisionInfoConfiguration;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Envers configuration.
 *
 * @author Chris Cranford
 */
public class Configuration {

	private static final String OPERATOR_IN = "in";
	private static final String OPERATOR_EQUALS = "=";

	private static final String DEFAULT_PREFIX = "";
	private static final String DEFAULT_SUFFIX = "_AUD";
	private static final String DEFAULT_MODIFIED_FLAG_SUFFIX = "_MOD";
	private static final String DEFAULT_ORIGINAL_ID = "originalId";
	private static final String DEFAULT_REV_FIELD = "REV";
	private static final String DEFAULT_REVTYPE_FIELD = "REVTYPE";
	private static final String DEFAULT_REVTYPE_TYPE = "byte";
	private static final String DEFAULT_REVEND_FIELD = "REVEND";
	private static final String DEFAULT_REV_TSTMP_FIELD = "REVEND_TSTMP";
	private static final String DEFAULT_SETORDINAL_FIELD = "SETORDINAL";

	private final EnversService enversService;

	private final String defaultCatalogName;
	private final String defaultSchemaName;
	private final String modifiedFlagsSuffix;
	private final String correlatedSubqueryOperator;

	private final Class<? extends RevisionListener> revisionListenerClass;

	private final AuditStrategy auditStrategy;
	private final ModifiedColumnNamingStrategy modifiedColumnNamingStrategy;

	private final boolean nativeIdEnabled;
	private final boolean allowIdentifierReuse;
	private final boolean generateRevisionsForCollections;
	private final boolean doNotAuditOptimisticLockingField;
	private final boolean storeDeleteData;
	private final boolean cascadeDeleteRevision;
	private final boolean modifiedFlagsEnabled;
	private final boolean modifiedFlagsDefined;
	private final boolean findByRevisionExactMatch;
	private final boolean globalLegacyRelationTargetNotFound;

	private final boolean trackEntitiesChanged;
	private boolean trackEntitiesOverride;

	private final String auditTablePrefix;
	private final String auditTableSuffix;
	private final String originalIdPropertyName;
	private final String revisionFieldName;
	private final String revisionNumberPath;
	private final String revisionPropertyBasePath;
	private final String revisionTypePropertyName;
	private final String revisionTypePropertyType;
	private final String revisionEndFieldName;
	private final String revisionEndTimestampFieldName;
	private final String embeddableSetOrdinalPropertyName;
	private final boolean revisionEndTimestampEnabled;
	private final boolean revisionEndTimestampNumeric;
	private final boolean revisionEndTimestampUseLegacyPlacement;
	private final boolean revisionSequenceNoCache;

	private final Map<String, String> customAuditTableNames = new HashMap<>();

	private final RevisionInfoConfiguration revisionInfo;

	public Configuration(Properties properties, EnversService enversService, InFlightMetadataCollector metadata) {
		this.enversService = enversService;

		final ConfigurationProperties configProps = new ConfigurationProperties( properties );

		defaultCatalogName = configProps.getString( EnversSettings.DEFAULT_CATALOG );
		defaultSchemaName = configProps.getString( EnversSettings.DEFAULT_SCHEMA );

		correlatedSubqueryOperator = resolveCorrelatedSubqueryOperator( properties );

		modifiedFlagsSuffix = configProps.getString( EnversSettings.MODIFIED_FLAG_SUFFIX, DEFAULT_MODIFIED_FLAG_SUFFIX );

		revisionListenerClass = resolveRevisionListener( configProps, enversService );

		final StrategySelector strategySelector = enversService.getServiceRegistry().getService( StrategySelector.class );
		modifiedColumnNamingStrategy = resolveModifiedColumnNamingStrategy( configProps, strategySelector );
		auditStrategy = resolveAuditStrategy( configProps, strategySelector );

		nativeIdEnabled = configProps.getBoolean( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, true );
		allowIdentifierReuse = configProps.getBoolean( EnversSettings.ALLOW_IDENTIFIER_REUSE, false );

		generateRevisionsForCollections = configProps.getBoolean( EnversSettings.REVISION_ON_COLLECTION_CHANGE, true );

		// todo: deprecate original in favor of enabling versioning optimistic locking as opt-in.
		doNotAuditOptimisticLockingField = configProps.getBoolean( EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD, true );

		storeDeleteData = configProps.getBoolean( EnversSettings.STORE_DATA_AT_DELETE, false );
		cascadeDeleteRevision = configProps.getBoolean( EnversSettings.CASCADE_DELETE_REVISION, false );
		trackEntitiesChanged = configProps.getBoolean( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, false );

		modifiedFlagsDefined = properties.get( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG ) != null;
		modifiedFlagsEnabled = configProps.getBoolean( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, false );

		findByRevisionExactMatch = configProps.getBoolean( EnversSettings.FIND_BY_REVISION_EXACT_MATCH, false );
		globalLegacyRelationTargetNotFound = configProps.getBoolean( EnversSettings.GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG, true );

		auditTablePrefix = configProps.getString( EnversSettings.AUDIT_TABLE_PREFIX, DEFAULT_PREFIX );
		auditTableSuffix = configProps.getString( EnversSettings.AUDIT_TABLE_SUFFIX, DEFAULT_SUFFIX );

		originalIdPropertyName = configProps.getString( EnversSettings.ORIGINAL_ID_PROP_NAME, DEFAULT_ORIGINAL_ID );
		revisionFieldName = configProps.getString( EnversSettings.REVISION_FIELD_NAME, DEFAULT_REV_FIELD );

		revisionTypePropertyName = configProps.getString( EnversSettings.REVISION_TYPE_FIELD_NAME, DEFAULT_REVTYPE_FIELD );
		revisionTypePropertyType = DEFAULT_REVTYPE_TYPE;

		revisionEndFieldName = configProps.getString(
				EnversSettings.AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME,
				DEFAULT_REVEND_FIELD
		);

		revisionEndTimestampEnabled = configProps.getBoolean(
				EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP,
				false
		);

		if ( revisionEndTimestampEnabled ) {
			revisionEndTimestampFieldName = configProps.getString(
					EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME,
					DEFAULT_REV_TSTMP_FIELD
			);
			revisionEndTimestampNumeric = configProps.getBoolean(
					EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_NUMERIC,
					false
			);
			revisionEndTimestampUseLegacyPlacement = configProps.getBoolean(
					EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_LEGACY_PLACEMENT,
					true
			);
		}
		else {
			revisionEndTimestampFieldName = null;
			revisionEndTimestampNumeric = false;
			revisionEndTimestampUseLegacyPlacement = true;
		}

		embeddableSetOrdinalPropertyName = configProps.getString(
				EnversSettings.EMBEDDABLE_SET_ORDINAL_FIELD_NAME,
				DEFAULT_SETORDINAL_FIELD
		);
		revisionSequenceNoCache = configProps.getBoolean(
				EnversSettings.REVISION_SEQUENCE_NOCACHE,
				false
		);

		revisionPropertyBasePath = originalIdPropertyName + "." + revisionFieldName + ".";
		revisionNumberPath = revisionPropertyBasePath + "id";

		this.revisionInfo = new RevisionInfoConfiguration( this, metadata );
	}

	public boolean isGenerateRevisionsForCollections() {
		return generateRevisionsForCollections;
	}

	public boolean isDoNotAuditOptimisticLockingField() {
		return doNotAuditOptimisticLockingField;
	}

	public boolean isStoreDataAtDelete() {
		return storeDeleteData;
	}

	public boolean isTrackEntitiesChanged() {
		return trackEntitiesOverride ? trackEntitiesOverride : trackEntitiesChanged;
	}

	public void setTrackEntitiesChanged(boolean trackEntitiesChanged) {
		this.trackEntitiesOverride = trackEntitiesChanged;
	}

	public boolean hasSettingForUseModifiedFlag() {
		return modifiedFlagsDefined;
	}

	public boolean isModifiedFlagsEnabled() {
		return modifiedFlagsEnabled;
	}

	public boolean isNativeIdEnabled() {
		return nativeIdEnabled;
	}

	public boolean isCascadeDeleteRevision() {
		return cascadeDeleteRevision;
	}

	public boolean isAllowIdentifierReuse() {
		return allowIdentifierReuse;
	}

	public boolean isFindByRevisionExactMatch() {
		return findByRevisionExactMatch;
	}

	public boolean isGlobalLegacyRelationTargetNotFound() {
		return globalLegacyRelationTargetNotFound;
	}

	public boolean isRevisionEndTimestampEnabled() {
		return revisionEndTimestampEnabled;
	}

	public boolean isRevisionEndTimestampNumeric() {
		return revisionEndTimestampNumeric;
	}

	public boolean isRevisionEndTimestampUseLegacyPlacement() {
		return revisionEndTimestampUseLegacyPlacement;
	}

	public boolean isRevisionSequenceNoCache() {
		return revisionSequenceNoCache;
	}

	public String getDefaultCatalogName() {
		return defaultCatalogName;
	}

	public String getDefaultSchemaName() {
		return defaultSchemaName;
	}

	public String getCorrelatedSubqueryOperator() {
		return correlatedSubqueryOperator;
	}

	public String getModifiedFlagsSuffix() {
		return modifiedFlagsSuffix;
	}

	public String getOriginalIdPropertyName() {
		return originalIdPropertyName;
	}

	public String getRevisionFieldName() {
		return revisionFieldName;
	}

	public String getRevisionEndTimestampFieldName() {
		return revisionEndTimestampFieldName;
	}

	public String getRevisionEndFieldName() {
		return revisionEndFieldName;
	}

	public String getRevisionNumberPath() {
		return revisionNumberPath;
	}

	/**
	 * todo: move this
	 * Get the revision property path.
	 *
	 * @param propertyName the property name within the revision entity
	 * @return path to the given property of the revision entity associated with the audited entity
	 */
	public String getRevisionPropertyPath(String propertyName) {
		return revisionPropertyBasePath + propertyName;
	}

	public String getRevisionTypePropertyName() {
		return revisionTypePropertyName;
	}

	public String getRevisionTypePropertyType() {
		return revisionTypePropertyType;
	}

	/**
	 * todo: move this
	 * Get the audit enttiy name.
	 *
	 * @param entityName the entity name
	 * @return the prefixed and suffixed audit entity name based on configuration
	 */
	public String getAuditEntityName(String entityName) {
		return auditTablePrefix + entityName + auditTableSuffix;
	}

	public void addCustomAuditTableName(String entityName, String tableName) {
		customAuditTableNames.put( entityName, tableName );
	}

	/**
	 * Gets the audit table name by looking up the entity in the defined custom tables and if not found
	 * returns a prefixed, suffixed audit table name.  In the latter case, the name is not registered.
	 *
	 * @param entityName the entity name
	 * @param tableName the table name
	 * @return the audit table name either from the custom defined tables or prefixed/suffixed inline
	 */
	public String getAuditTableName(String entityName, String tableName) {
		final String existingName = customAuditTableNames.get( entityName );
		if ( existingName != null ) {
			return existingName;
		}
		return getAuditEntityName( tableName );
	}

	public String getAuditStrategyName() {
		return auditStrategy.getClass().getName();
	}

	public String getEmbeddableSetOrdinalPropertyName() {
		return embeddableSetOrdinalPropertyName;
	}

	public Class<? extends RevisionListener> getRevisionListenerClass() {
		return revisionListenerClass;
	}

	public AuditStrategy getAuditStrategy() {
		return auditStrategy;
	}

	public ModifiedColumnNamingStrategy getModifiedColumnNamingStrategy() {
		return modifiedColumnNamingStrategy;
	}

	public RevisionInfoConfiguration getRevisionInfo() {
		return revisionInfo;
	}

	/**
	 * Returns a reference to the {@link EnversService}.
	 * This method is not recommended and discouraged, will be removed in a future release.
	 *
	 * @return the envers service
	 */
	public EnversService getEnversService() {
		return enversService;
	}

	private static String resolveCorrelatedSubqueryOperator(Properties properties) {
		if ( HSQLDialect.class.getName().equals( properties.get( Environment.DIALECT ) ) ) {
			return OPERATOR_IN;
		}
		return OPERATOR_EQUALS;
	}

	private static Class<? extends RevisionListener> resolveRevisionListener(
			ConfigurationProperties configProps,
			EnversService enversService) {
		final String className = configProps.getString( EnversSettings.REVISION_LISTENER );
		if ( !StringTools.isEmpty( className ) ) {
			try {
				return ReflectionTools.loadClass( className, enversService.getClassLoaderService() );
			}
			catch (ClassLoadingException e) {
				throw new EnversMappingException( "Revision listener class not found: " + className + "." );
			}
		}
		return null;
	}

	private static ModifiedColumnNamingStrategy resolveModifiedColumnNamingStrategy(
			ConfigurationProperties configProps,
			StrategySelector selector) {
		return selector.resolveDefaultableStrategy(
				ModifiedColumnNamingStrategy.class,
				configProps.getString( EnversSettings.MODIFIED_COLUMN_NAMING_STRATEGY ),
				(Callable<ModifiedColumnNamingStrategy>) () -> selector.resolveDefaultableStrategy(
						ModifiedColumnNamingStrategy.class,
						"default",
						new LegacyModifiedColumnNamingStrategy()
				)
		);
	}

	private static AuditStrategy resolveAuditStrategy(ConfigurationProperties configProps, StrategySelector selector) {
		return selector.resolveDefaultableStrategy(
				AuditStrategy.class,
				configProps.getString( EnversSettings.AUDIT_STRATEGY, DefaultAuditStrategy.class.getName() ),
				(Callable<AuditStrategy>) () -> new DefaultAuditStrategy()
		);
	}

	private static class ConfigurationProperties {
		private final Properties properties;

		private ConfigurationProperties(Properties properties) {
			this.properties = properties;
		}

		String getString(String propertyName) {
			return ConfigurationHelper.getString( propertyName, properties );
		}

		String getString(String propertyName, String defaultValue) {
			return ConfigurationHelper.getString( propertyName, properties, defaultValue );
		}

		boolean getBoolean(String propertyName, boolean defaultValue) {
			return ConfigurationHelper.getBoolean( propertyName, properties, defaultValue );
		}

		boolean getBooleanWithFallback(String basePropertyName, String newPropertyName, boolean defaultValue) {
			if ( !properties.containsKey( basePropertyName ) ) {
				return getBoolean( newPropertyName, defaultValue );
			}
			return ConfigurationHelper.getBoolean( basePropertyName, properties );
		}
	}
}
