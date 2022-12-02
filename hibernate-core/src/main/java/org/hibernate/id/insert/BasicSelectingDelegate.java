/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;

import static org.hibernate.id.IdentifierGeneratorHelper.getGeneratedIdentity;

/**
 * Delegate for dealing with IDENTITY columns where the dialect requires an
 * additional command execution to retrieve the generated IDENTITY value
 */
public class BasicSelectingDelegate extends AbstractSelectingDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;

	public BasicSelectingDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
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
			SessionFactoryImplementor factory) {
		final TableInsertBuilder builder =
				new TableInsertBuilderStandard( persister, persister.getIdentifierTableMapping(), factory );

		IdentityColumnSupport identityColumnSupport = dialect.getIdentityColumnSupport();
		if ( identityColumnSupport.hasIdentityInsertKeyword() ) {
			builder.addKeyColumn(
					identifierMapping.getSelectionExpression(),
					identityColumnSupport.getIdentityInsertString(),
					identifierMapping.getJdbcMapping()
			);
		}

		return builder;
	}

	@Override
	protected String getSelectSQL() {
		return persister.getIdentitySelectString();
	}

	@Override
	protected Object extractGeneratedValue(Object entity, ResultSet rs, SharedSessionContractImplementor session)
			throws SQLException {
		return getGeneratedIdentity(
				rs,
				persister.getNavigableRole(),
				persister.getRootTableKeyColumnNames()[0],
				persister.getIdentifierType(),
				session.getJdbcServices().getJdbcEnvironment().getDialect()
		);
	}
}
