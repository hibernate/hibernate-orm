/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;

/**
 * Specialized {@link IdentifierGeneratingInsert} which appends the database
 * specific clause which signifies to return generated {@code IDENTITY} values
 * to the end of the insert statement.
 * 
 * @author Christian Beikov
 */
public class SybaseJConnGetGeneratedKeysDelegate extends GetGeneratedKeysDelegate {
	/**
	 * @deprecated Use {@link #SybaseJConnGetGeneratedKeysDelegate(EntityPersister)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	public SybaseJConnGetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		this( persister );
	}

	public SybaseJConnGetGeneratedKeysDelegate(EntityPersister persister) {
		super( persister, true, EventType.INSERT );
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		return dialect().getIdentityColumnSupport().appendIdentitySelectToInsert(
				( (BasicEntityIdentifierMapping) persister.getRootEntityDescriptor().getIdentifierMapping() ).getSelectionExpression(),
				insertSQL
		);
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
			return getGeneratedValues( preparedStatement, resultSet, persister, getTiming(), session );
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
