/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;

/// Internal delegate which appends the Dialect's identity-selection SQL to an
/// insert and consumes the resulting row.
///
/// @since 8.0
/// @author Christian Beikov
/// @author Steve Ebersole
@Internal
public class AppendingIdentitySelectDelegate extends GetGeneratedKeysDelegate {

	public AppendingIdentitySelectDelegate(EntityPersister persister) {
		super( persister, true, EventType.INSERT );
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		final var identifierMapping =
				(BasicEntityIdentifierMapping)
						persister.getRootEntityDescriptor().getIdentifierMapping();
		return dialect().getIdentityColumnSupport()
				.appendIdentitySelectToInsert( identifierMapping.getSelectionExpression(), insertSQL );
	}

	@Override
	public GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		final var resultSet =
				session.getJdbcCoordinator().getResultSetReturn()
						.execute( preparedStatement, sql );
		try {
			return getGeneratedValues( resultSet, preparedStatement, persister, getTiming(), session );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated keys from ResultSet",
					sql
			);
		}
	}
}
