/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;

/**
 * Specialized {@link GetGeneratedKeysDelegate} which appends the database
 * specific clause which signifies to return generated {@code IDENTITY} values
 * to the end of the insert statement.
 *
 * @author Christian Beikov
 */
public class SybaseJConnGetGeneratedKeysDelegate extends GetGeneratedKeysDelegate {

	public SybaseJConnGetGeneratedKeysDelegate(EntityPersister persister) {
		super( persister, true, EventType.INSERT );
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		final BasicEntityIdentifierMapping identifierMapping =
				(BasicEntityIdentifierMapping) persister.getRootEntityDescriptor().getIdentifierMapping();
		return dialect().getIdentityColumnSupport()
				.appendIdentitySelectToInsert( identifierMapping.getSelectionExpression(), insertSQL );
	}

	@Override
	public GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		ResultSet resultSet = jdbcCoordinator.getResultSetReturn().execute( preparedStatement, sql );
		try {
			return getGeneratedValues( resultSet, preparedStatement, persister, getTiming(), session );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated-keys ResultSet",
					sql
			);
		}
	}
}
