package org.hibernate.dialect.lock;

import org.hibernate.persister.entity.Lockable;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.StaleObjectStateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.exception.JDBCExceptionHelper;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A locking strategy where the locks are obtained through select statements.
 * <p/>
 * For non-read locks, this is achieved through the Dialect's specific
 * SELECT ... FOR UPDATE syntax.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(org.hibernate.LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(org.hibernate.LockMode, String)
 * @since 3.2
 *
 * @author Steve Ebersole
 */
public class SelectLockingStrategy implements LockingStrategy {

	private final Lockable lockable;
	private final LockMode lockMode;
	private final String sql;

	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indictates the type of lock to be acquired.
	 */
	public SelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		this.sql = generateLockString();
	}

	/**
	 * @see LockingStrategy#lock
	 */
	public void lock(
	        Serializable id,
	        Object version,
	        Object object,
	        SessionImplementor session) throws StaleObjectStateException, JDBCException {

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
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not lock: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
					sql
				);
		}
	}

	protected LockMode getLockMode() {
		return lockMode;
	}

	protected String generateLockString() {
		SessionFactoryImplementor factory = lockable.getFactory();
		SimpleSelect select = new SimpleSelect( factory.getDialect() )
				.setLockMode( lockMode )
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
