/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Parameter bind specification used for optimistic lock version seeding (from insert statements).
 *
 * @author Steve Ebersole
 */
public class VersionTypeSeedParameterSpecification extends AbstractJdbcParameter {
	private final EntityVersionMapping versionMapping;

	/**
	 * Constructs a version seed parameter bind specification.
	 *
	 * @param versionMapping The version mapping.
	 */
	public VersionTypeSeedParameterSpecification(EntityVersionMapping versionMapping) {
		super( versionMapping.getJdbcMapping() );
		this.versionMapping = versionMapping;
	}

	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParamBindings,
			ExecutionContext executionContext) throws SQLException {
		//noinspection unchecked
		getJdbcMapping().getJdbcValueBinder().bind(
				statement,
				versionMapping.getJavaType().seed(
						versionMapping.getLength(),
						versionMapping.getTemporalPrecision() != null
								? versionMapping.getTemporalPrecision()
								: versionMapping.getPrecision(),
						versionMapping.getScale(),
						executionContext.getSession()
				),
				startPosition,
				executionContext.getSession()
		);
	}
}
