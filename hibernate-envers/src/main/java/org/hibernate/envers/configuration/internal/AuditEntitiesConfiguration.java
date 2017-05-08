/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Configuration of versions entities - names of fields, entities and tables created to store versioning information.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Stephanie Pau at Markit Group Plc
 * @author Chris Cranford
 */
public class  AuditEntitiesConfiguration {
	private final String auditTablePrefix;
	private final String auditTableSuffix;

	private final String auditStrategyName;
	private final String originalIdPropName;

	private final String revisionFieldName;
	private final String revisionNumberPath;
	private final String revisionPropBasePath;

	private final String revisionTypePropName;
	private final String revisionTypePropType;

	private final String revisionInfoEntityName;

	private final Map<String, String> customAuditTablesNames;

	private final String revisionEndFieldName;

	private final boolean revisionEndTimestampEnabled;
	private final String revisionEndTimestampFieldName;

	private final String embeddableSetOrdinalPropertyName;
	private final EnversService enversService;

	public AuditEntitiesConfiguration(
			Properties properties,
			String revisionInfoEntityName,
			EnversService enversService) {
		this.revisionInfoEntityName = revisionInfoEntityName;
		this.enversService = enversService;

		auditTablePrefix = ConfigurationHelper.getString( EnversSettings.AUDIT_TABLE_PREFIX, properties, "" );
		auditTableSuffix = ConfigurationHelper.getString( EnversSettings.AUDIT_TABLE_SUFFIX, properties, "_AUD" );

		auditStrategyName = ConfigurationHelper.getString(
				EnversSettings.AUDIT_STRATEGY, properties, DefaultAuditStrategy.class.getName()
		);

		originalIdPropName = ConfigurationHelper.getString(
				EnversSettings.ORIGINAL_ID_PROP_NAME, properties, "originalId"
		);

		revisionFieldName = ConfigurationHelper.getString( EnversSettings.REVISION_FIELD_NAME, properties, "REV" );

		revisionTypePropName = ConfigurationHelper.getString(
				EnversSettings.REVISION_TYPE_FIELD_NAME, properties, "REVTYPE"
		);
		revisionTypePropType = "byte";

		revisionEndFieldName = ConfigurationHelper.getString(
				EnversSettings.AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME, properties, "REVEND"
		);

		revisionEndTimestampEnabled = ConfigurationHelper.getBoolean(
				EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, properties, false
		);

		if ( revisionEndTimestampEnabled ) {
			revisionEndTimestampFieldName = ConfigurationHelper.getString(
					EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME, properties, "REVEND_TSTMP"
			);
		}
		else {
			revisionEndTimestampFieldName = null;
		}

		customAuditTablesNames = new HashMap<>();

		revisionNumberPath = originalIdPropName + "." + revisionFieldName + ".id";
		revisionPropBasePath = originalIdPropName + "." + revisionFieldName + ".";

		embeddableSetOrdinalPropertyName = ConfigurationHelper.getString(
				EnversSettings.EMBEDDABLE_SET_ORDINAL_FIELD_NAME, properties, "SETORDINAL"
		);
	}

	public String getOriginalIdPropName() {
		return originalIdPropName;
	}

	public String getRevisionFieldName() {
		return revisionFieldName;
	}

	public boolean isRevisionEndTimestampEnabled() {
		return revisionEndTimestampEnabled;
	}

	public String getRevisionEndTimestampFieldName() {
		return revisionEndTimestampFieldName;
	}

	public String getRevisionNumberPath() {
		return revisionNumberPath;
	}

	/**
	 * @param propertyName Property of the revision entity.
	 *
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

	public void addCustomAuditTableName(String entityName, String tableName) {
		customAuditTablesNames.put( entityName, tableName );
	}

	public String getAuditEntityName(String entityName) {
		return auditTablePrefix + entityName + auditTableSuffix;
	}

	public String getAuditTableName(String entityName, String tableName) {
		final String customHistoryTableName = customAuditTablesNames.get( entityName );
		if ( customHistoryTableName == null ) {
			return auditTablePrefix + tableName + auditTableSuffix;
		}

		return customHistoryTableName;
	}

	public String getAuditStrategyName() {
		return auditStrategyName;
	}

	public String getRevisionEndFieldName() {
		return revisionEndFieldName;
	}

	public String getEmbeddableSetOrdinalPropertyName() {
		return embeddableSetOrdinalPropertyName;
	}

	/**
	 * @deprecated (since 5.2.1), while actually added in 5.2.1, this was added to cleanup the
	 * audit strategy interface temporarily.
	 */
	@Deprecated
	public EnversService getEnversService() {
		return enversService;
	}
}
