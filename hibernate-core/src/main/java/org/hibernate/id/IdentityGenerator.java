/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.insert.AbstractReturningDelegate;
import org.hibernate.id.insert.AbstractSelectingDelegate;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.insert.InsertSelectIdentityInsert;

/**
 * A generator for use with ANSI-SQL IDENTITY columns used as the primary key.
 * The IdentityGenerator for autoincrement/identity key generation.
 * <br><br>
 * Indicates to the <tt>Session</tt> that identity (ie. identity/autoincrement
 * column) key generation should be used.
 *
 * @author Christoph Sturm
 */
public class IdentityGenerator extends AbstractPostInsertGenerator {

	@Override
	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect,
			boolean isGetGeneratedKeysEnabled) throws HibernateException {
		if ( isGetGeneratedKeysEnabled ) {
			return dialect.getIdentityColumnSupport().buildGetGeneratedKeysDelegate( persister, dialect );
		}
		else if ( dialect.getIdentityColumnSupport().supportsInsertSelectIdentity() ) {
			return new InsertSelectDelegate( persister, dialect );
		}
		else {
			return new BasicDelegate( persister, dialect );
		}
	}



	/**
	 * Delegate for dealing with IDENTITY columns where the dialect supports returning
	 * the generated IDENTITY value directly from the insert statement.
	 */
	public static class InsertSelectDelegate
			extends AbstractReturningDelegate
			implements InsertGeneratedIdentifierDelegate {
		private final PostInsertIdentityPersister persister;
		private final Dialect dialect;

		public InsertSelectDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
			super( persister );
			this.persister = persister;
			this.dialect = dialect;
		}

		@Override
		public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
			InsertSelectIdentityInsert insert = new InsertSelectIdentityInsert( dialect );
			insert.addIdentityColumn( persister.getRootTableKeyColumnNames()[0] );
			return insert;
		}

		@Override
		protected PreparedStatement prepare(String insertSQL, SharedSessionContractImplementor session) throws SQLException {
			return session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( insertSQL, PreparedStatement.NO_GENERATED_KEYS );
		}

		@Override
		public Serializable executeAndExtract(PreparedStatement insert, SharedSessionContractImplementor session)
				throws SQLException {
			ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().execute( insert );
			try {
				return IdentifierGeneratorHelper.getGeneratedIdentity(
						rs,
						persister.getRootTableKeyColumnNames()[0],
						persister.getIdentifierType(),
						session.getJdbcServices().getJdbcEnvironment().getDialect()
				);
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, insert );
			}
		}

		public Serializable determineGeneratedIdentifier(SharedSessionContractImplementor session, Object entity) {
			throw new AssertionFailure( "insert statement returns generated value" );
		}
	}

	/**
	 * Delegate for dealing with IDENTITY columns where the dialect requires an
	 * additional command execution to retrieve the generated IDENTITY value
	 */
	public static class BasicDelegate
			extends AbstractSelectingDelegate
			implements InsertGeneratedIdentifierDelegate {
		private final PostInsertIdentityPersister persister;
		private final Dialect dialect;

		public BasicDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
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
		protected String getSelectSQL() {
			return persister.getIdentitySelectString();
		}

		@Override
		protected Serializable getResult(
				SharedSessionContractImplementor session,
				ResultSet rs,
				Object object) throws SQLException {
			return IdentifierGeneratorHelper.getGeneratedIdentity(
					rs,
					persister.getRootTableKeyColumnNames()[0],
					persister.getIdentifierType(),
					session.getJdbcServices().getJdbcEnvironment().getDialect()
			);
		}
	}

}
