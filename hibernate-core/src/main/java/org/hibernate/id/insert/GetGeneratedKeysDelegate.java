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
import java.util.Locale;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.MutationStatementPreparer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;

/**
 * Delegate for dealing with IDENTITY columns using JDBC3 getGeneratedKeys
 *
 * @author Andrea Boriero
 */
public class GetGeneratedKeysDelegate extends AbstractReturningDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;

	public GetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		super( persister );
		this.persister = persister;
		this.dialect = dialect;
	}

	@Override
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
		IdentifierGeneratingInsert insert = new IdentifierGeneratingInsert( dialect );
		insert.addIdentityColumn( persister.getRootTableKeyColumnNames()[0] );
		return insert;
	}

	@Override
	public TableInsertBuilder createTableInsertBuilder(
			BasicEntityIdentifierMapping identifierMapping,
			Expectation expectation,
			SessionFactoryImplementor sessionFactory) {
		final TableInsertBuilder builder = new TableInsertBuilderStandard(
				persister,
				persister.getIdentifierTableMapping(),
				sessionFactory
		);

		final String value = dialect.getIdentityColumnSupport().getIdentityInsertString();
		if ( value != null ) {
			builder.addKeyColumn( persister.getRootTableKeyColumnNames()[0], value, identifierMapping.getJdbcMapping() );
		}

		return builder;
	}

	@Override
	public PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final MutationStatementPreparer statementPreparer = jdbcCoordinator.getMutationStatementPreparer();
		return statementPreparer.prepareStatement(
				insertSql,
				PreparedStatement.RETURN_GENERATED_KEYS
		);
	}


	@Override
	public Object performInsert(
			PreparedStatementDetails insertStatementDetails,
			JdbcValueBindings jdbcValueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		final JdbcServices jdbcServices = session.getJdbcServices();
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();

		final String insertSql = insertStatementDetails.getSqlString();

		jdbcServices.getSqlStatementLogger().logStatement( insertSql );

		final PreparedStatement insertStatement = insertStatementDetails.resolveStatement();
		jdbcValueBindings.beforeStatement( insertStatementDetails, session );

		try {
			jdbcCoordinator.getResultSetReturn().executeUpdate( insertStatement );

			try {
				final ResultSet rs = insertStatement.getGeneratedKeys();
				try {
					return IdentifierGeneratorHelper.getGeneratedIdentity(
							rs,
							persister.getNavigableRole(),
							persister.getRootTableKeyColumnNames()[ 0 ],
							persister.getIdentifierType(),
							jdbcServices.getJdbcEnvironment().getDialect()
					);
				}
				catch (SQLException e) {
					throw jdbcServices.getSqlExceptionHelper().convert(
							e,
							() -> String.format(
									Locale.ROOT,
									"Unable to extract generated key from generated-key for `%s`",
									persister.getNavigableRole().getFullPath()
							),
							insertSql
					);
				}
				finally {
					if ( rs != null ) {
						jdbcCoordinator
								.getLogicalConnection()
								.getResourceRegistry()
								.release( rs, insertStatement );
					}
				}
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( insertStatement );
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated-keys ResultSet",
					insertSql
			);
		}
	}

	@Override
	public Object executeAndExtract(
			String insertSql,
			PreparedStatement insertStatement,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		jdbcCoordinator.getResultSetReturn().executeUpdate( insertStatement );

		try {
			final ResultSet rs = insertStatement.getGeneratedKeys();
			try {
				return IdentifierGeneratorHelper.getGeneratedIdentity(
						rs,
						persister.getNavigableRole(),
						persister.getRootTableKeyColumnNames()[0],
						persister.getIdentifierType(),
						jdbcServices.getJdbcEnvironment().getDialect()
				);
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Unable to extract generated key(s) from generated-keys ResultSet",
						insertSql
				);
			}
			finally {
				if ( rs != null ) {
					jdbcCoordinator
							.getLogicalConnection()
							.getResourceRegistry()
							.release( rs, insertStatement );
				}
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated-keys ResultSet",
					insertSql
			);
		}
	}
}
