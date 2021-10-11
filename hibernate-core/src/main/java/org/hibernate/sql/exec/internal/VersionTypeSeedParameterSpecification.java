/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.descriptor.java.VersionJavaType;

/**
 * Parameter bind specification used for optimistic lock version seeding (from insert statements).
 *
 * @author Steve Ebersole
 */
public class VersionTypeSeedParameterSpecification extends JdbcParameterImpl {
	private final VersionJavaType<?> type;

	/**
	 * Constructs a version seed parameter bind specification.
	 *
	 * @param type The version type.
	 */
	public VersionTypeSeedParameterSpecification(JdbcMapping jdbcMapping, VersionJavaType<?> type) {
		super( jdbcMapping );
		this.type = type;
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
				type.seed( executionContext.getSession() ),
				startPosition,
				executionContext.getSession()
		);
	}
}
