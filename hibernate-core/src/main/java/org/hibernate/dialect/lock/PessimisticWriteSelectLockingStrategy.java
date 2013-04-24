/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.SimpleSelect;

/**
 * A pessimistic locking strategy where the locks are obtained through select statements.
 * <p/>
 * For non-read locks, this is achieved through the Dialect's specific
 * SELECT ... FOR UPDATE syntax.
 *
 * This strategy is valid for LockMode.PESSIMISTIC_WRITE
 *
 * This class is a clone of SelectLockingStrategy.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(org.hibernate.LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(org.hibernate.LockMode, String)
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 * @since 3.5
 */
public class PessimisticWriteSelectLockingStrategy extends AbstractSelectLockingStrategy {
	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public PessimisticWriteSelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		super( lockable, lockMode );
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session) {
		final String sql = determineSql( timeout );
		final SessionFactoryImplementor factory = session.getFactory();
		try {
			try {
				final PreparedStatement st = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
				try {
					getLockable().getIdentifierType().nullSafeSet( st, id, 1, session );
					if ( getLockable().isVersioned() ) {
						getLockable().getVersionType().nullSafeSet(
								st,
								version,
								getLockable().getIdentifierType().getColumnSpan( factory ) + 1,
								session
						);
					}

					final ResultSet rs = session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st );
					try {
						if ( !rs.next() ) {
							if ( factory.getStatistics().isStatisticsEnabled() ) {
								factory.getStatisticsImplementor()
										.optimisticFailure( getLockable().getEntityName() );
							}
							throw new StaleObjectStateException( getLockable().getEntityName(), id );
						}
					}
					finally {
						session.getTransactionCoordinator().getJdbcCoordinator().release( rs, st );
					}
				}
				finally {
					session.getTransactionCoordinator().getJdbcCoordinator().release( st );
				}
			}
			catch ( SQLException e ) {
				throw session.getFactory().getSQLExceptionHelper().convert(
						e,
						"could not lock: " + MessageHelper.infoString( getLockable(), id, session.getFactory() ),
						sql
				);
			}
		}
		catch (JDBCException e) {
			throw new PessimisticEntityLockException( object, "could not obtain pessimistic lock", e );
		}
	}

	protected String generateLockString(int lockTimeout) {
		final SessionFactoryImplementor factory = getLockable().getFactory();
		final LockOptions lockOptions = new LockOptions( getLockMode() );
		lockOptions.setTimeOut( lockTimeout );
		final SimpleSelect select = new SimpleSelect( factory.getDialect() )
				.setLockOptions( lockOptions )
				.setTableName( getLockable().getRootTableName() )
				.addColumn( getLockable().getRootTableIdentifierColumnNames()[0] )
				.addCondition( getLockable().getRootTableIdentifierColumnNames(), "=?" );
		if ( getLockable().isVersioned() ) {
			select.addCondition( getLockable().getVersionColumnName(), "=?" );
		}
		if ( factory.getSettings().isCommentsEnabled() ) {
			select.setComment( getLockMode() + " lock " + getLockable().getEntityName() );
		}
		return select.toStatementString();
	}
}
