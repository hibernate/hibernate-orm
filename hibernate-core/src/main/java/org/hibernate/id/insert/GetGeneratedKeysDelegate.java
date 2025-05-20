/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.MutationStatementPreparer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValueBasicResultBuilder;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getActualGeneratedModelPart;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;
import static org.hibernate.internal.util.StringHelper.unquote;

/**
 * Delegate for dealing with generated values using the JDBC3 method
 * {@link PreparedStatement#getGeneratedKeys()}.
 * <p>
 * Supports both {@link EventType#INSERT insert} and {@link EventType#UPDATE update} statements.
 *
 * @author Andrea Boriero
 */
public class GetGeneratedKeysDelegate extends AbstractReturningDelegate {
	private final String[] columnNames;

	/**
	 * @deprecated Use {@link #GetGeneratedKeysDelegate(EntityPersister, boolean, EventType)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	public GetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect, boolean inferredKeys) {
		this( persister, inferredKeys, EventType.INSERT );
	}

	public GetGeneratedKeysDelegate(
			EntityPersister persister,
			boolean inferredKeys,
			EventType timing) {
		super( persister, timing, !inferredKeys, false );

		if ( inferredKeys ) {
			columnNames = null;
		}
		else {
			final List<GeneratedValueBasicResultBuilder> resultBuilders = jdbcValuesMappingProducer.getResultBuilders();
			final List<String> columnNamesList = new ArrayList<>( resultBuilders.size() );
			final boolean unquote = dialect().unquoteGetGeneratedKeys();
			for ( GeneratedValueBasicResultBuilder resultBuilder : resultBuilders ) {
				final String col = getActualGeneratedModelPart( resultBuilder.getModelPart() ).getSelectionExpression();
				columnNamesList.add( unquote ? unquote( col, dialect() ) : col );
			}
			columnNames = columnNamesList.toArray( new String[0] );
		}
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor factory) {
		if ( getTiming() == EventType.INSERT ) {
			return new TableInsertBuilderStandard( persister, persister.getIdentifierTableMapping(), factory );
		}
		else {
			return new TableUpdateBuilderStandard<>( persister, persister.getIdentifierTableMapping(), factory );
		}
	}

	@Override
	public PreparedStatement prepareStatement(String sql, SharedSessionContractImplementor session) {
		MutationStatementPreparer preparer = session.getJdbcCoordinator().getMutationStatementPreparer();
		return columnNames == null
				? preparer.prepareStatement( sql, RETURN_GENERATED_KEYS )
				: preparer.prepareStatement( sql, columnNames );
	}

	@Override
	public GeneratedValues performMutation(
			PreparedStatementDetails statementDetails,
			JdbcValueBindings jdbcValueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		final JdbcServices jdbcServices = session.getJdbcServices();
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();

		final String sql = statementDetails.getSqlString();

		jdbcServices.getSqlStatementLogger().logStatement( sql );

		try {
			final PreparedStatement preparedStatement = statementDetails.resolveStatement();
			jdbcValueBindings.beforeStatement( statementDetails );

			jdbcCoordinator.getResultSetReturn().executeUpdate( preparedStatement, sql );

			try {
				final ResultSet resultSet = preparedStatement.getGeneratedKeys();
				try {
					return getGeneratedValues( resultSet, persister, getTiming(), session );
				}
				catch (SQLException e) {
					throw jdbcServices.getSqlExceptionHelper().convert(
							e,
							() -> String.format(
									Locale.ROOT,
									"Unable to extract generated key from generated-key for `%s`",
									persister.getNavigableRole().getFullPath()
							),
							sql
					);
				}
				finally {
					if ( resultSet != null ) {
						jdbcCoordinator
								.getLogicalConnection()
								.getResourceRegistry()
								.release( resultSet, preparedStatement );
					}
				}
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Unable to extract generated-keys ResultSet",
						sql
				);
			}
		}
		finally {
			if ( statementDetails.getStatement() != null ) {
				statementDetails.releaseStatement( session );
			}
			jdbcValueBindings.afterStatement( statementDetails.getMutatingTableDetails() );
		}
	}

	@Override
	public GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		jdbcCoordinator.getResultSetReturn().executeUpdate( preparedStatement, sql );

		try {
			final ResultSet resultSet = preparedStatement.getGeneratedKeys();
			try {
				return getGeneratedValues( resultSet, persister, getTiming(), session );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Unable to extract generated key(s) from generated-keys ResultSet",
						sql
				);
			}
			finally {
				if ( resultSet != null ) {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( resultSet, preparedStatement );
				}
			}
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
