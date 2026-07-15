/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.annotations.Audited;
import org.hibernate.mapping.PersistentClass;

import static org.hibernate.boot.model.internal.AuditHelper.findFirstAuditOverrideForProperty;

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

	static AuditTableConfig fromAnnotationOverrides(PersistentClass owner, String propertyName) {
		var firstOverride = findFirstAuditOverrideForProperty( owner, propertyName );
		if ( firstOverride == null ) {
			return DEFAULT;
		}
		var collectionTable = firstOverride.collectionTable();
		return new AuditTableConfig( collectionTable.name(), collectionTable.schema(), collectionTable.catalog(),
				Audited.Table.DEFAULT_CHANGESET_ID_COLUMN_NAME, Audited.Table.DEFAULT_MODIFICATION_TYPE_COLUMN_NAME,
				Audited.Table.DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME );

	}

}
