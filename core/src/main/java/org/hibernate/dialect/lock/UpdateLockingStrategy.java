package org.hibernate.dialect.lock;

import org.hibernate.persister.entity.Lockable;
import org.hibernate.LockMode;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.JDBCException;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.sql.Update;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A locking strategy where the locks are obtained through update statements.
 * <p/>
 * This strategy is not valid for read style locks.
 *
 * @since 3.2
 *
 * @author Steve Ebersole
 */
public class UpdateLockingStrategy implements LockingStrategy {
	private static final Log log = LogFactory.getLog( UpdateLockingStrategy.class );

	private final Lockable lockable;
	private final LockMode lockMode;
	private final String sql;

	/**
	 * Construct a locking strategy based on SQL UPDATE statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indictates the type of lock to be acquired.  Note that
	 * read-locks are not valid for this strategy.
	 */
	public UpdateLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.UPGRADE ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for update statement" );
		}
		if ( !lockable.isVersioned() ) {
			log.warn( "write locks via update not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
			this.sql = null;
		}
		else {
			this.sql = generateLockString();
		}
	}

	/**
	 * @see LockingStrategy#lock
	 */
	public void lock(
			Serializable id,
	        Object version,
	        Object object,
	        SessionImplementor session) throws StaleObjectStateException, JDBCException {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "write locks via update not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}
		// todo : should we additionally check the current isolation mode explicitly?
		SessionFactoryImplementor factory = session.getFactory();
		try {
			PreparedStatement st = session.getBatcher().prepareSelectStatement( sql );
			try {
				lockable.getVersionType().nullSafeSet( st, version, 1, session );
				int offset = 2;

				lockable.getIdentifierType().nullSafeSet( st, id, offset, session );
				offset += lockable.getIdentifierType().getColumnSpan( factory );

				if ( lockable.isVersioned() ) {
					lockable.getVersionType().nullSafeSet( st, version, offset, session );
				}

				int affected = st.executeUpdate();
				if ( affected < 0 ) {
					factory.getStatisticsImplementor().optimisticFailure( lockable.getEntityName() );
					throw new StaleObjectStateException( lockable.getEntityName(), id );
				}

			}
			finally {
				session.getBatcher().closeStatement( st );
			}

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
			        sqle,
			        "could not lock: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
			        sql
			);
		}
	}

	protected String generateLockString() {
		SessionFactoryImplementor factory = lockable.getFactory();
		Update update = new Update( factory.getDialect() );
		update.setTableName( lockable.getRootTableName() );
		update.setPrimaryKeyColumnNames( lockable.getRootTableIdentifierColumnNames() );
		update.setVersionColumnName( lockable.getVersionColumnName() );
		update.addColumn( lockable.getVersionColumnName() );
		if ( factory.getSettings().isCommentsEnabled() ) {
			update.setComment( lockMode + " lock " + lockable.getEntityName() );
		}
		return update.toStatementString();
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
