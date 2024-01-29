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

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.internal.CoreLogging;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.hibernate.id.IdentifierGeneratorHelper.getGeneratedIdentity;

/**
 * Specialized {@link IdentifierGeneratingInsert} which appends the database
 * specific clause which signifies to return generated {@code IDENTITY} values
 * to the end of the insert statement.
 * 
 * @author Christian Beikov
 */
public class SybaseJConnGetGeneratedKeysDelegate extends GetGeneratedKeysDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;

	public SybaseJConnGetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		super( persister, dialect, true );
		this.persister = persister;
		this.dialect = dialect;
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		return dialect.getIdentityColumnSupport().appendIdentitySelectToInsert( insertSQL );
	}

	@Override
	public Object executeAndExtract(
			String insertSql,
			PreparedStatement insertStatement,
			SharedSessionContractImplementor session) {
		JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		ResultSet resultSet = jdbcCoordinator.getResultSetReturn().execute( insertStatement, insertSql );
		try {
			return getGeneratedIdentity( persister.getNavigableRole().getFullPath(), resultSet, persister, session );
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
