/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.annotations.Audited;

import java.util.Map;

/**
 * Universal audit table configuration that can be created from different sources.
 *
 * @author Niklas Enns
 */

public record AuditTableConfig(String name, String schema, String catalog, String changesetIdColumn,
							String modificationTypeColumn, String invalidatingChangesetIdColumn) {

	public static final AuditTableConfig DEFAULT = new AuditTableConfig( "", "", "",
			Audited.Table.DEFAULT_CHANGESET_ID_COLUMN_NAME, Audited.Table.DEFAULT_MODIFICATION_TYPE_COLUMN_NAME,
			Audited.Table.DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME );

	static AuditTableConfig fromAuditedTableAnnotation(Audited.Table table) {
		if ( table == null ) {
			return DEFAULT;

		}
		return new AuditTableConfig( table.name(), table.schema(), table.catalog(), table.changesetIdColumn(),
				table.modificationTypeColumn(), table.invalidatingChangesetIdColumn() );
	}

	static AuditTableConfig fromAnnotationOverrides(String propertyName, Map<String, Audited.Override> effectiveAuditOverride) {
		var auditOverride = effectiveAuditOverride.get( propertyName );
		if ( auditOverride == null ) {
			return DEFAULT;
		}
		var collectionTable = auditOverride.collectionTable();
		return new AuditTableConfig( collectionTable.name(), collectionTable.schema(), collectionTable.catalog(),
				Audited.Table.DEFAULT_CHANGESET_ID_COLUMN_NAME, Audited.Table.DEFAULT_MODIFICATION_TYPE_COLUMN_NAME,
				Audited.Table.DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME );

	}

}
