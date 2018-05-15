/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.Lockable;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * A pessimistic locking strategy where the locks are obtained through update statements.
 * <p/>
 * This strategy is valid for LockMode.PESSIMISTIC_READ
 *
 * This class is a clone of UpdateLockingStrategy.
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 * @since 3.5
 */
public class PessimisticReadUpdateLockingStrategy implements LockingStrategy {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			PessimisticReadUpdateLockingStrategy.class.getName()
	);

	private final Lockable lockable;
	private final LockMode lockMode;
	private final String sql;

	/**
	 * Construct a locking strategy based on SQL UPDATE statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.  Note that
	 * read-locks are not valid for this strategy.
	 */
	public PessimisticReadUpdateLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.PESSIMISTIC_READ ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for update statement" );
		}
		if ( StringHelper.isEmpty( lockable.getVersionColumnName() ) ) {
			LOG.writeLocksNotSupported( lockable.getEntityName() );
			this.sql = null;
		}
		else {
			this.sql = generateLockString();
		}
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SharedSessionContractImplementor session) {
		if ( StringHelper.isEmpty( lockable.getVersionColumnName() ) ) {
			throw new HibernateException( "write locks via update not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}

		final ExecutionContext executionContext = new BasicExecutionContext( session );
		final SessionFactoryImplementor factory = session.getFactory();
		final TypeConfiguration typeConfiguration = factory.getTypeConfiguration();

		try {
			final PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			try {
				final AtomicInteger count = new AtomicInteger();

				final VersionDescriptor<Object, Object> versionDescriptor = lockable.getHierarchy().getVersionDescriptor();

				versionDescriptor.dehydrate(
						versionDescriptor.unresolve( version, session ),
						(jdbcValue, type, boundColumn) -> {
							try {
								type.getJdbcValueBinder().bind( st, count.getAndIncrement(), jdbcValue, executionContext );
							}
							catch (SQLException e) {
								throw session.getJdbcServices().getSqlExceptionHelper().convert(
										e,
										"Could not bind version value(s) to lock entity: " +
												MessageHelper.infoString( lockable, id, session.getFactory() ),
										sql
								);
							}
						},
						Clause.WHERE,
						session
				);

				final EntityIdentifier<Object, Object> identifierDescriptor = lockable.getHierarchy().getIdentifierDescriptor();
				identifierDescriptor.dehydrate(
						identifierDescriptor.unresolve( id, session ),
						(jdbcValue, type, boundColumn) -> {
							try {
								type.getJdbcValueBinder().bind(
										st,
										count.getAndIncrement(),
										jdbcValue,
										executionContext
								);
							}
							catch (SQLException e) {
								throw session.getJdbcServices().getSqlExceptionHelper().convert(
										e,
										"Could not bind id value(s) to lock entity: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
										sql
								);
							}
						},
						Clause.WHERE,
						session
				);

				final int affected = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( st );
				// todo:  should this instead check for exactly one row modified?
				if ( affected < 0 ) {
					if (factory.getStatistics().isStatisticsEnabled()) {
						factory.getStatistics().optimisticFailure( lockable.getEntityName() );
					}
					throw new StaleObjectStateException( lockable.getEntityName(), id );
				}

			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (JDBCException e) {
			throw new PessimisticEntityLockException( object, "could not obtain pessimistic lock", e );
		}
	}

	protected String generateLockString() {
		final SessionFactoryImplementor factory = lockable.getFactory();
		final Update update = new Update( factory.getDialect() );
		update.setTableName( lockable.getRootTableName() );
		update.addPrimaryKeyColumns( lockable.getRootTableIdentifierColumnNames() );
		update.setVersionColumnName( lockable.getVersionColumnName() );
		update.addColumn( lockable.getVersionColumnName() );
		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
			update.setComment( lockMode + " lock " + lockable.getEntityName() );
		}
		return update.toStatementString();
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
