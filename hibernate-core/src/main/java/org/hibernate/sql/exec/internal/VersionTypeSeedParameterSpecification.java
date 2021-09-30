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
import org.hibernate.type.VersionType;

/**
 * Parameter bind specification used for optimistic lock version seeding (from insert statements).
 *
 * @author Steve Ebersole
 */
public class VersionTypeSeedParameterSpecification extends JdbcParameterImpl {
	private final VersionType type;

	/**
	 * Constructs a version seed parameter bind specification.
	 *
	 * @param type The version type.
	 */
	public VersionTypeSeedParameterSpecification(VersionType type) {
		super( (JdbcMapping) type );
		this.type = type;
	}

	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParamBindings,
			ExecutionContext executionContext) throws SQLException {
		type.nullSafeSet( statement, type.seed( executionContext.getSession() ), startPosition, executionContext.getSession() );
	}
}
