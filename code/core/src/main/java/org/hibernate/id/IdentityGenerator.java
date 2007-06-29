//$Id: IdentityGenerator.java 9681 2006-03-24 18:10:04Z steve.ebersole@jboss.com $
package org.hibernate.id;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.AbstractSelectingDelegate;
import org.hibernate.id.insert.AbstractReturningDelegate;
import org.hibernate.id.insert.InsertSelectIdentityInsert;
import org.hibernate.dialect.Dialect;
import org.hibernate.HibernateException;
import org.hibernate.AssertionFailure;
import org.hibernate.util.GetGeneratedKeysHelper;


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

	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
	        Dialect dialect,
	        boolean isGetGeneratedKeysEnabled) throws HibernateException {
		if ( isGetGeneratedKeysEnabled ) {
			return new GetGeneratedKeysDelegate( persister, dialect );
		}
		else if ( dialect.supportsInsertSelectIdentity() ) {
			return new InsertSelectDelegate( persister, dialect );
		}
		else {
			return new BasicDelegate( persister, dialect );
		}
	}

	/**
	 * Delegate for dealing with IDENTITY columns using JDBC3 getGeneratedKeys
	 */
	public static class GetGeneratedKeysDelegate
			extends AbstractReturningDelegate
			implements InsertGeneratedIdentifierDelegate {
		private final PostInsertIdentityPersister persister;
		private final Dialect dialect;

		public GetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
			super( persister );
			this.persister = persister;
			this.dialect = dialect;
		}

		public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
			IdentifierGeneratingInsert insert = new IdentifierGeneratingInsert( dialect );
			insert.addIdentityColumn( persister.getRootTableKeyColumnNames()[0] );
			return insert;
		}

		protected PreparedStatement prepare(String insertSQL, SessionImplementor session) throws SQLException {
			return session.getBatcher().prepareStatement( insertSQL, true );
		}

		public Serializable executeAndExtract(PreparedStatement insert) throws SQLException {
			insert.executeUpdate();
			return IdentifierGeneratorFactory.getGeneratedIdentity(
					GetGeneratedKeysHelper.getGeneratedKey( insert ),
			        persister.getIdentifierType()
			);
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

		public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
			InsertSelectIdentityInsert insert = new InsertSelectIdentityInsert( dialect );
			insert.addIdentityColumn( persister.getRootTableKeyColumnNames()[0] );
			return insert;
		}

		protected PreparedStatement prepare(String insertSQL, SessionImplementor session) throws SQLException {
			return session.getBatcher().prepareStatement( insertSQL, false );
		}

		public Serializable executeAndExtract(PreparedStatement insert) throws SQLException {
			if ( !insert.execute() ) {
				while ( !insert.getMoreResults() && insert.getUpdateCount() != -1 ) {
					// do nothing until we hit the rsult set containing the generated id
				}
			}
			ResultSet rs = insert.getResultSet();
			try {
				return IdentifierGeneratorFactory.getGeneratedIdentity( rs, persister.getIdentifierType() );
			}
			finally {
				rs.close();
			}
		}

		public Serializable determineGeneratedIdentifier(SessionImplementor session, Object entity) {
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

		public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
			IdentifierGeneratingInsert insert = new IdentifierGeneratingInsert( dialect );
			insert.addIdentityColumn( persister.getRootTableKeyColumnNames()[0] );
			return insert;
		}

		protected String getSelectSQL() {
			return persister.getIdentitySelectString();
		}

		protected Serializable getResult(
				SessionImplementor session,
		        ResultSet rs,
		        Object object) throws SQLException {
			return IdentifierGeneratorFactory.getGeneratedIdentity( rs, persister.getIdentifierType() );
		}
	}

}
