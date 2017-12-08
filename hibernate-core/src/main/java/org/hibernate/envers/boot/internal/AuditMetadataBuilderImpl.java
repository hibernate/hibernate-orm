/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.boot.AuditMetadataBuilder;
import org.hibernate.envers.boot.spi.AuditMetadataBuilderImplementor;
import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.envers.boot.spi.AuditMetadataImplementor;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.configuration.internal.EntitiesConfigurator;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.envers.configuration.internal.RevisionInfoConfiguration;
import org.hibernate.envers.configuration.internal.RevisionInfoConfigurationBuilder;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Chris Cranford
 * @since 6.0
 */
public class AuditMetadataBuilderImpl implements AuditMetadataBuilderImplementor {

	private final InFlightMetadataCollector metadata;
	private final AuditMetadataBuildingOptionsImpl options;
	private RevisionInfoConfiguration revisionInfoConfiguration;
	private EntitiesConfigurations entitiesConfigurations;

	public AuditMetadataBuilderImpl(InFlightMetadataCollector metadata) {
		this.metadata = metadata;
		this.options = new AuditMetadataBuildingOptionsImpl( metadata.getMetadataBuildingOptions().getServiceRegistry() );
	}

	@Override
	public AuditMetadataBuildingOptions getAuditMetadataBuildingOptions() {
		return options;
	}

	@Override
	public AuditMetadataBuilder applyMappingCollector(MappingCollector mappingCollector) {
		buildMappings( mappingCollector );
		return this;
	}

	@Override
	public AuditMetadataBuilder applyTrackEntitiesChangedInRevision(boolean trackEntitiesChangedInRevision) {
		this.options.trackEntitiesChangedInRevision = trackEntitiesChangedInRevision;
		return this;
	}

	@Override
	public AuditMetadataBuilder applyRevisionInfoEntityName(String revisionInfoEntityName) {
		this.options.revisionInfoEntityName = revisionInfoEntityName;
		return this;
	}

	@Override
	public AuditMetadataImplementor build() {
		return new AuditMetadataImpl( options, revisionInfoConfiguration, entitiesConfigurations );
	}

	private void buildMappings(MappingCollector mappingCollector) {
		this.revisionInfoConfiguration = new RevisionInfoConfigurationBuilder( metadata, this ).build();

		// this has to be resolved because of configurations for entities.
		options.auditStrategy = options.serviceRegistry.getService( StrategySelector.class ).resolveStrategy(
				AuditStrategy.class,
				options.auditStrategyName,
				new Callable<AuditStrategy>() {
					@Override
					public AuditStrategy call() throws Exception {
						return new DefaultAuditStrategy();
					}
				},
				new StrategyCreatorAuditStrategyImpl(
						revisionInfoConfiguration.getRevisionInfoTimestampData(),
						revisionInfoConfiguration.getRevisionInfoClass(),
						options.serviceRegistry
				)
		);

		// build entity bindings
		this.entitiesConfigurations = new EntitiesConfigurator( this ).build(
				metadata,
				mappingCollector,
				revisionInfoConfiguration.getRevisionInfoXmlMapping(),
				revisionInfoConfiguration.getRevisionInfoRelationMapping()
		);
	}

	private Class<?> resolveAuditStrategyClass(String auditStrategyName, ServiceRegistry serviceRegistry) {
		try {
			return AuditMetadataBuilderImpl.class.getClassLoader().loadClass( auditStrategyName );
		}
		catch ( Exception e ) {
			final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
			return ReflectionTools.loadClass( auditStrategyName, cls );
		}
	}

	public static class AuditMetadataBuildingOptionsImpl implements AuditMetadataBuildingOptions {

		private final ServiceRegistry serviceRegistry;
		private final boolean generateRevisionsForCollections;
		private final boolean doNotAuditOptimsiticLockingField;
		private final boolean storeDataAtDelete;
		private final String defaultSchemaName;
		private final String defaultCatalogName;
		private final boolean globalWithModifiedFlag;
		private final boolean hasGlobalSettingForWithModifiedFlag;
		private final boolean globalLegacyRelationTargetNotFound;
		private final String modifiedFlagSuffix;
		private final boolean useRevisionEntityWithNativeId;
		private final boolean cascadeDeleteRevision;
		private final boolean allowIdentifierReuse;
		private final String correlatedSubqueryOperator;
		private final String auditTablePrefix;
		private final String auditTableSuffix;
		private final String auditStrategyName;
		private final String originalIdPropertyName;
		private final String revisionFieldName;
		private final String revisionNumberPath;
		private final String revisionPropertyBasePath;
		private final String revisionTypePropertyName;
		private final String revisionTypePropertyType;
		private final String revisionEndFieldName;
		private final String revisionEndTimestampFieldName;
		private final boolean revisionEndTimestampEnabled;
		private final boolean numericRevisionEndTimestampEnabled;
		private final boolean revisionEndTimestampLegacyBehaviorEnabled;
		private final String embeddableSetOrdinalPropertyName;

		private boolean trackEntitiesChangedInRevision;
		private String revisionInfoEntityName;
		private String revisionListenerClassName;

		private AuditStrategy auditStrategy;
		private Class<? extends RevisionListener> revisionListenerClass;
		private final Map<String, String> customAuditTablesNames;

		public AuditMetadataBuildingOptionsImpl(ServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;
			this.customAuditTablesNames = new HashMap<>();

			final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
			final Map properties = cfgService.getSettings();

			this.generateRevisionsForCollections = ConfigurationHelper.getBoolean(
					EnversSettings.REVISION_ON_COLLECTION_CHANGE,
					properties,
					true
			);

			this.doNotAuditOptimsiticLockingField = ConfigurationHelper.getBoolean(
					EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD,
					properties,
					true
			);

			this.storeDataAtDelete = ConfigurationHelper.getBoolean(
					EnversSettings.STORE_DATA_AT_DELETE,
					properties,
					false
			);

			this.defaultSchemaName = (String) properties.get( EnversSettings.DEFAULT_SCHEMA );
			this.defaultCatalogName = (String) properties.get( EnversSettings.DEFAULT_CATALOG );

			if ( HSQLDialect.class.getName().equals( properties.get( Environment.DIALECT ) ) ) {
				this.correlatedSubqueryOperator = "in";
			}
			else {
				this.correlatedSubqueryOperator = "=";
			}

			this.trackEntitiesChangedInRevision = ConfigurationHelper.getBoolean(
					EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION,
					properties,
					false
			);

			this.cascadeDeleteRevision = ConfigurationHelper.getBoolean(
					EnversSettings.CASCADE_DELETE_REVISION,
					properties,
					false
			);

			this.useRevisionEntityWithNativeId = ConfigurationHelper.getBoolean(
					EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID,
					properties,
					true
			);

			this.hasGlobalSettingForWithModifiedFlag = properties.get( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG ) != null;
			this.globalWithModifiedFlag = ConfigurationHelper.getBoolean(
					EnversSettings.GLOBAL_WITH_MODIFIED_FLAG,
					properties,
					false
			);

			this.globalLegacyRelationTargetNotFound = ConfigurationHelper.getBoolean(
					EnversSettings.GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG,
					properties,
					true
			);

			this.modifiedFlagSuffix = ConfigurationHelper.getString(
					EnversSettings.MODIFIED_FLAG_SUFFIX,
					properties,
					"_MOD"
			);

			this.allowIdentifierReuse = ConfigurationHelper.getBoolean(
					EnversSettings.ALLOW_IDENTIFIER_REUSE,
					properties,
					false
			);

			this.revisionListenerClassName = (String) properties.get( EnversSettings.REVISION_LISTENER );
			if ( this.revisionListenerClassName != null ) {
				try {
					this.revisionListenerClass = ReflectionTools.loadClass(
							this.revisionListenerClassName,
							serviceRegistry.getService( ClassLoaderService.class )
					);
				}
				catch ( ClassLoadingException e ) {
					throw new MappingException( "Revision listener class not found: " + revisionListenerClassName, e );
				}
			}

			this.auditTablePrefix = ConfigurationHelper.getString(
					EnversSettings.AUDIT_TABLE_PREFIX,
					properties,
					""
			);

			this.auditTableSuffix = ConfigurationHelper.getString(
					EnversSettings.AUDIT_TABLE_SUFFIX,
					properties,
					"_AUD"
			);

			this.auditStrategyName = ConfigurationHelper.getString(
					EnversSettings.AUDIT_STRATEGY,
					properties,
					AuditStrategy.getDefaultStrategyName()
			);

			this.originalIdPropertyName = "originalId";

			this.revisionFieldName = ConfigurationHelper.getString(
					EnversSettings.REVISION_FIELD_NAME,
					properties,
					"REV"
			);

			this.revisionTypePropertyName = ConfigurationHelper.getString(
					EnversSettings.REVISION_TYPE_FIELD_NAME,
					properties,
					"REVTYPE"
			);

			this.revisionTypePropertyType = "byte";

			this.revisionEndFieldName = ConfigurationHelper.getString(
					EnversSettings.AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME,
					properties,
					"REVEND"
			);

			this.revisionEndTimestampEnabled = ConfigurationHelper.getBoolean(
					EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP,
					properties,
					false
			);

			if ( this.revisionEndTimestampEnabled ) {
				this.numericRevisionEndTimestampEnabled = ConfigurationHelper.getBoolean(
						EnversSettings.USE_NUMERIC_REVEND_TIMESTAMP_FIELD_TYPE,
						properties,
						false
				);
			}
			else {
				this.numericRevisionEndTimestampEnabled = false;
			}

			this.embeddableSetOrdinalPropertyName = ConfigurationHelper.getString(
					EnversSettings.EMBEDDABLE_SET_ORDINAL_FIELD_NAME,
					properties,
					"SETORDINAL"
			);

			if ( this.revisionEndTimestampEnabled ) {
				this.revisionEndTimestampFieldName = ConfigurationHelper.getString(
						EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME,
						properties,
						"REVEND_TSTMP"
				);
			}
			else {
				revisionEndTimestampFieldName = null;
			}

			// new behavior is enabled by default
			this.revisionEndTimestampLegacyBehaviorEnabled = ConfigurationHelper.getBoolean(
					EnversSettings.AUDIT_STRATEGY_VALIDITY_LEGACY_REVEND_TIMESTAMP,
					properties,
					false
			);

			this.revisionPropertyBasePath = originalIdPropertyName + "." + revisionFieldName + ".";
			this.revisionNumberPath = this.revisionPropertyBasePath + "id";

		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}

		@Override
		public boolean isRevisionOnCollectionChangeEnabled() {
			return generateRevisionsForCollections;
		}

		@Override
		public boolean isDoNotAuditOptimisticLockingFieldEnabled() {
			return doNotAuditOptimsiticLockingField;
		}

		@Override
		public boolean isStoreDataAtDeleteEnabled() {
			return storeDataAtDelete;
		}

		@Override
		public boolean isTrackEntitiesChangedInRevisionEnabled() {
			return trackEntitiesChangedInRevision;
		}

		@Override
		public boolean isGlobalWithModifiedFlagEnabled() {
			return globalWithModifiedFlag;
		}

		@Override
		public boolean isGlobalLegacyRelationTargetNotFoundEnabled() {
			return globalLegacyRelationTargetNotFound;
		}

		@Override
		public boolean hasGlobalWithModifiedFlag() {
			return hasGlobalSettingForWithModifiedFlag;
		}

		@Override
		public boolean isUseRevisionEntityWithNativeIdEnabled() {
			return useRevisionEntityWithNativeId;
		}

		@Override
		public boolean isCascadeDeleteRevisionEnabled() {
			return cascadeDeleteRevision;
		}

		@Override
		public boolean isAllowIdentifierReuseEnabled() {
			return allowIdentifierReuse;
		}

		@Override
		public String getDefaultSchemaName() {
			return defaultSchemaName;
		}

		@Override
		public String getDefaultCatalogName() {
			return defaultCatalogName;
		}

		@Override
		public String getCorrelatedSubqueryOperator() {
			return correlatedSubqueryOperator;
		}

		@Override
		public String getModifiedFlagSuffix() {
			return modifiedFlagSuffix;
		}

		@Override
		public String getRevisionInfoEntityName() {
			return revisionInfoEntityName;
		}

		@Override
		public AuditStrategy getAuditStrategy() {
			return auditStrategy;
		}

		@Override
		public Class<? extends RevisionListener> getRevisionListenerClass() {
			return revisionListenerClass;
		}

		@Override
		public String getOriginalIdPropName() {
			return originalIdPropertyName;
		}

		@Override
		public String getRevisionFieldName() {
			return revisionFieldName;
		}

		@Override
		public boolean isRevisionEndTimestampEnabled() {
			return revisionEndTimestampEnabled;
		}

		@Override
		public boolean isRevisionEndTimestampLegacyBehaviorEnabled() {
			return revisionEndTimestampLegacyBehaviorEnabled;
		}

		@Override
		public String getRevisionEndTimestampFieldName() {
			return revisionEndTimestampFieldName;
		}

		@Override
		public boolean isNumericRevisionEndTimestampEnabled() {
			return numericRevisionEndTimestampEnabled;
		}

		@Override
		public String getRevisionNumberPath() {
			return revisionNumberPath;
		}

		@Override
		public String getRevisionTypePropName() {
			return revisionTypePropertyName;
		}

		@Override
		public String getRevisionTypePropType() {
			return revisionTypePropertyType;
		}

		@Override
		public String getRevisionEndFieldName() {
			return revisionEndFieldName;
		}

		@Override
		public String getEmbeddableSetOrdinalPropertyName() {
			return embeddableSetOrdinalPropertyName;
		}

		@Override
		public String getAuditEntityName(String entityName) {
			return auditTablePrefix + entityName + auditTableSuffix;
		}

		@Override
		public void addCustomAuditTableName(String entityName, String tableName) {
			customAuditTablesNames.put( entityName, tableName );
		}

		@Override
		public String getAuditTableName(String entityName, String tableName) {
			final String customHistoryTableName = customAuditTablesNames.get( entityName );
			if ( customHistoryTableName == null ) {
				return auditTablePrefix + tableName + auditTableSuffix;
			}

			return customHistoryTableName;
		}

		@Override
		public String getRevisionPropertyPath(String propertyName) {
			return revisionPropertyBasePath + propertyName;
		}
	}
}
