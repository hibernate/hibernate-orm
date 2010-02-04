/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.dialect.lock;

import org.hibernate.LockOptions;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.JDBCException;
import org.hibernate.PessimisticLockException;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A pessimistic locking strategy where the locks are obtained through select statements.
 * <p/>
 * For non-read locks, this is achieved through the Dialect's specific
 * SELECT ... FOR UPDATE syntax.
 *
 * This strategy is valid for LockMode.PESSIMISTIC_READ
 *
 * This class is a clone of SelectLockingStrategy.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(org.hibernate.LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(org.hibernate.LockMode, String)
 * @since 3.5
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 */
public class PessimisticReadSelectLockingStrategy implements LockingStrategy {

	private final Lockable lockable;
	private final LockMode lockMode;
	private final String sql;

	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indictates the type of lock to be acquired.
	 */
	public PessimisticReadSelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		this.sql = generateLockString(LockOptions.WAIT_FOREVER);
	}

   /**
	 * @see org.hibernate.dialect.lock.LockingStrategy#lock
	 */
	public void lock(
      Serializable id,
      Object version,
      Object object,
      int timeout, SessionImplementor session) throws StaleObjectStateException, JDBCException {
		String sql = this.sql;
		if ( timeout == LockOptions.NO_WAIT ) {
			sql = generateLockString( LockOptions.NO_WAIT );
		}
		else if ( timeout > 0) {
			sql = generateLockString( timeout );
		}

		SessionFactoryImplementor factory = session.getFactory();
		try {
			PreparedStatement st = session.getBatcher().prepareSelectStatement( sql );
			try {
				lockable.getIdentifierType().nullSafeSet( st, id, 1, session );
				if ( lockable.isVersioned() ) {
					lockable.getVersionType().nullSafeSet(
							st,
							version,
							lockable.getIdentifierType().getColumnSpan( factory ) + 1,
							session
					);
				}

				ResultSet rs = st.executeQuery();
				try {
					if ( !rs.next() ) {
						if ( factory.getStatistics().isStatisticsEnabled() ) {
							factory.getStatisticsImplementor()
									.optimisticFailure( lockable.getEntityName() );
						}
						throw new StaleObjectStateException( lockable.getEntityName(), id );
					}
				}
				finally {
					rs.close();
				}
			}
			finally {
				session.getBatcher().closeStatement( st );
			}

		}
		catch ( SQLException sqle ) {
			JDBCException e = JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not lock: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
					sql
				);
			throw new PessimisticLockException("could not obtain pessimistic lock", e, object);
		}
	}

	protected LockMode getLockMode() {
		return lockMode;
	}

	protected String generateLockString(int lockTimeout) {
		SessionFactoryImplementor factory = lockable.getFactory();
		LockOptions lockOptions = new LockOptions(this.lockMode);
		lockOptions.setTimeOut( lockTimeout );
		SimpleSelect select = new SimpleSelect( factory.getDialect() )
				.setLockOptions( lockOptions )
				.setTableName( lockable.getRootTableName() )
				.addColumn( lockable.getRootTableIdentifierColumnNames()[0] )
				.addCondition( lockable.getRootTableIdentifierColumnNames(), "=?" );
		if ( lockable.isVersioned() ) {
			select.addCondition( lockable.getVersionColumnName(), "=?" );
		}
		if ( factory.getSettings().isCommentsEnabled() ) {
			select.setComment( lockMode + " lock " + lockable.getEntityName() );
		}
		return select.toStatementString();
	}
}