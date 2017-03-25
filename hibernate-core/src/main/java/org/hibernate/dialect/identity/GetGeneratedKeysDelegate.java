/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.AbstractReturningDelegate;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;

/**
 * Delegate for dealing with IDENTITY columns using JDBC3 getGeneratedKeys
 *
 * @author Andrea Boriero
 */
public class GetGeneratedKeysDelegate
		extends AbstractReturningDelegate
		implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;

	public GetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		super( persister );
		this.persister = persister;
		this.dialect = dialect;
	}

	@Override
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
		IdentifierGeneratingInsert insert = new IdentifierGeneratingInsert( dialect );
		insert.addIdentityColumn( persister.getRootTableKeyColumnNames()[0] );
		return insert;
	}

	@Override
	protected PreparedStatement prepare(String insertSQL, SharedSessionContractImplementor session) throws SQLException {
		return session
				.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( insertSQL, PreparedStatement.RETURN_GENERATED_KEYS );
	}

	@Override
	public Serializable executeAndExtract(PreparedStatement insert, SharedSessionContractImplementor session)
			throws SQLException {
		session.getJdbcCoordinator().getResultSetReturn().executeUpdate( insert );
		ResultSet rs = null;
		try {
			rs = insert.getGeneratedKeys();
			return IdentifierGeneratorHelper.getGeneratedIdentity(
					rs,
					persister.getRootTableKeyColumnNames()[0],
					persister.getIdentifierType(),
					session.getJdbcServices().getJdbcEnvironment().getDialect()
			);
		}
		finally {
			if ( rs != null ) {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, insert );
			}
		}
	}
}
