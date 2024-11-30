/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLJsonPGObjectJsonType extends AbstractPostgreSQLJsonPGObjectType {
	public PostgreSQLJsonPGObjectJsonType() {
		this( null, false );
	}
	private PostgreSQLJsonPGObjectJsonType(EmbeddableMappingType embeddableMappingType, boolean jsonb) {
		super( embeddableMappingType, jsonb );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new PostgreSQLJsonPGObjectJsonType( mappingType, false );
	}
}
