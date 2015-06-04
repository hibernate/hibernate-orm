/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.insert.AbstractReturningDelegate;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.Insert;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * A generator which combines sequence generation with immediate retrieval
 * through JDBC3 {@link java.sql.Connection#prepareStatement(String, String[]) getGeneratedKeys}.
 * In this respect it works much like ANSI-SQL IDENTITY generation.
 * <p/>
 * This generator only known to work with newer Oracle drivers compiled for
 * JDK 1.4 (JDBC3).
 * <p/>
 * Note: Due to a bug in Oracle drivers, sql comments on these insert statements
 * are completely disabled.
 *
 * @author Steve Ebersole
 * @deprecated See deprecation discussion on {@link SequenceGenerator}
 */
@Deprecated
public class SequenceIdentityGenerator
		extends SequenceGenerator
		implements PostInsertIdentifierGenerator {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SequenceIdentityGenerator.class.getName()
	);

	@Override
	public Serializable generate(SessionImplementor s, Object obj) {
		return IdentifierGeneratorHelper.POST_INSERT_INDICATOR;
	}

	@Override
	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect,
			boolean isGetGeneratedKeysEnabled) throws HibernateException {
		return new Delegate( persister, dialect, getSequenceName() );
	}

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		super.configure( type, params, serviceRegistry );
	}

	public static class Delegate extends AbstractReturningDelegate {
		private final Dialect dialect;
		private final String sequenceNextValFragment;
		private final String[] keyColumns;

		public Delegate(PostInsertIdentityPersister persister, Dialect dialect, String sequenceName) {
			super( persister );
			this.dialect = dialect;
			this.sequenceNextValFragment = dialect.getSelectSequenceNextValString( sequenceName );
			this.keyColumns = getPersister().getRootTableKeyColumnNames();
			if ( keyColumns.length > 1 ) {
				throw new HibernateException( "sequence-identity generator cannot be used with with multi-column keys" );
			}
		}

		public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
			NoCommentsInsert insert = new NoCommentsInsert( dialect );
			insert.addColumn( getPersister().getRootTableKeyColumnNames()[0], sequenceNextValFragment );
			return insert;
		}

		@Override
		protected PreparedStatement prepare(String insertSQL, SessionImplementor session) throws SQLException {
			return session.getJdbcCoordinator().getStatementPreparer().prepareStatement( insertSQL, keyColumns );
		}

		@Override
		protected Serializable executeAndExtract(PreparedStatement insert, SessionImplementor session)
				throws SQLException {
			session.getJdbcCoordinator().getResultSetReturn().executeUpdate( insert );
			return IdentifierGeneratorHelper.getGeneratedIdentity(
					insert.getGeneratedKeys(),
					getPersister().getRootTableKeyColumnNames()[0],
					getPersister().getIdentifierType()
			);
		}
	}

	public static class NoCommentsInsert extends IdentifierGeneratingInsert {
		public NoCommentsInsert(Dialect dialect) {
			super( dialect );
		}

		@Override
		public Insert setComment(String comment) {
			// don't allow comments on these insert statements as comments totally
			// blow up the Oracle getGeneratedKeys "support" :(
			LOG.disallowingInsertStatementComment();
			return this;
		}
	}
}
