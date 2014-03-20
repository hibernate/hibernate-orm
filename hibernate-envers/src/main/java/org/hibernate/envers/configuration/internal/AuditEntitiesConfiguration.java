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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.service.ServiceRegistry;

/**
 * Configuration of versions entities - names of fields, entities and tables created to store versioning information.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Stephanie Pau at Markit Group Plc
 */
public class AuditEntitiesConfiguration {
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

	public AuditEntitiesConfiguration(ServiceRegistry serviceRegistry,  String revisionInfoEntityName) {
		this.revisionInfoEntityName = revisionInfoEntityName;

		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );

		auditTablePrefix = configurationService.getSetting(
				EnversSettings.AUDIT_TABLE_PREFIX, StandardConverters.STRING, ""
		);
		auditTableSuffix = configurationService.getSetting(
				EnversSettings.AUDIT_TABLE_SUFFIX, StandardConverters.STRING, "_AUD"
		);

		auditStrategyName = configurationService.getSetting(
				EnversSettings.AUDIT_STRATEGY, StandardConverters.STRING, DefaultAuditStrategy.class.getName()
		);

		originalIdPropName = "originalId";

		revisionFieldName = configurationService.getSetting(
				EnversSettings.REVISION_FIELD_NAME, StandardConverters.STRING, "REV"
		);

		revisionTypePropName = configurationService.getSetting(
				EnversSettings.REVISION_TYPE_FIELD_NAME, StandardConverters.STRING, "REVTYPE"
		);
		revisionTypePropType = "byte";

		revisionEndFieldName = configurationService.getSetting(
				EnversSettings.AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME, StandardConverters.STRING, "REVEND"
		);

		revisionEndTimestampEnabled = configurationService.getSetting(
				EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP,
				StandardConverters.BOOLEAN,
				false
		);

		if ( revisionEndTimestampEnabled ) {
			revisionEndTimestampFieldName = configurationService.getSetting(
					EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME,
					StandardConverters.STRING,
					"REVEND_TSTMP"
			);
		}
		else {
			revisionEndTimestampFieldName = null;
		}

		customAuditTablesNames = new HashMap<String, String>();

		revisionNumberPath = originalIdPropName + "." + revisionFieldName + ".id";
		revisionPropBasePath = originalIdPropName + "." + revisionFieldName + ".";

		embeddableSetOrdinalPropertyName = configurationService.getSetting(
				EnversSettings.EMBEDDABLE_SET_ORDINAL_FIELD_NAME, StandardConverters.STRING, "SETORDINAL"
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
}
