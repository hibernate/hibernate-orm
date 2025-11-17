/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.envers.boot.model.BasicAttribute;
import org.hibernate.envers.boot.model.Column;
import org.hibernate.envers.boot.spi.ModifiedColumnNamingStrategy;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;

/**
 * @author Chris Cranford
 * @since 6.0
 */
public abstract class AbstractModifiedColumnNamingStrategy implements ModifiedColumnNamingStrategy {

	protected BasicAttribute createModifiedFlagAttribute(
			PropertyAuditingData propertyAuditingData,
			Configuration configuration,
			String columnName) {
		return createModifiedFlagAttribute(
				propertyAuditingData.getName(),
				configuration.getModifiedFlagsSuffix(),
				propertyAuditingData.getModifiedFlagName(),
				columnName
		);
	}

	protected BasicAttribute createModifiedFlagAttribute(String name, String suffix, String flagName, String columnName) {
		final BasicAttribute attribute = new BasicAttribute(
				( flagName != null ) ? flagName : ModifiedColumnNameResolver.getName( name, suffix ),
				"boolean",
				true,
				false,
				false
		);
		attribute.addColumn( new Column(columnName ) );
		return attribute;
	}

}
