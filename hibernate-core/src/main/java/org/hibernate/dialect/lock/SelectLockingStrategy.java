/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.Lockable;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A locking strategy where the locks are obtained through select statements.
 * <p/>
 * For non-read locks, this is achieved through the Dialect's specific
 * SELECT ... FOR UPDATE syntax.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(org.hibernate.LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(org.hibernate.LockMode, String)
 *
 * @author Steve Ebersole
 * @since 3.2
 */
public class SelectLockingStrategy extends AbstractSelectLockingStrategy {
	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indictates the type of lock to be acquired.
	 */
	public SelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		super( lockable, lockMode );
	}

	@Override
	public void lock(
			Serializable id,
			Object version,
			Object object,
			int timeout,
			SharedSessionContractImplementor session) throws StaleObjectStateException, JDBCException {
		final String sql = determineSql( timeout );

		final ExecutionContext executionContext = new BasicExecutionContext( session );
		final SessionFactoryImplementor factory = session.getFactory();
		final TypeConfiguration typeConfiguration = factory.getTypeConfiguration();

		try {
			final PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			try {
				final AtomicInteger count = new AtomicInteger();

				final EntityIdentifier<Object, Object> identifierDescriptor = getLockable().getHierarchy().getIdentifierDescriptor();
				identifierDescriptor.dehydrate(
//						identifierDescriptor.unresolve( id, session ),
						id,
						(jdbcValue, type, boundColumn) -> {
							try {
								type.getJdbcValueBinder().bind( st, count.getAndIncrement(), jdbcValue, executionContext );
							}
							catch (SQLException e) {
								throw session.getJdbcServices().getSqlExceptionHelper().convert(
										e,
										"Could not bind id value(s) to lock entity: " + MessageHelper.infoString( getLockable(), id, session.getFactory() ),
										sql
								);
							}
						},
						Clause.WHERE,
						session
				);


				final VersionDescriptor<Object, Object> versionDescriptor = getLockable().getHierarchy().getVersionDescriptor();
				if ( versionDescriptor != null ) {
					versionDescriptor.dehydrate(
							versionDescriptor.unresolve( version, session ),
							(jdbcValue, type, boundColumn) -> {
								try {
									type.getJdbcValueBinder().bind( st, count.getAndIncrement(), jdbcValue, executionContext );
								}
								catch (SQLException e) {
									throw session.getJdbcServices().getSqlExceptionHelper().convert(
											e,
											"Could not bind version value(s) to lock entity: " + MessageHelper.infoString( getLockable(), id, session.getFactory() ),
											sql
									);
								}
							},
							Clause.WHERE,
							session
					);
				}

				final ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st );
				try {
					if ( !rs.next() ) {
						if ( factory.getStatistics().isStatisticsEnabled() ) {
							factory.getStatistics().optimisticFailure( getLockable().getEntityName() );
						}
						throw new StaleObjectStateException( getLockable().getEntityName(), id );
					}
				}
				finally {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}

		}
		catch ( SQLException sqle ) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not lock: " + MessageHelper.infoString( getLockable(), id, session.getFactory() ),
					sql
				);
		}
	}

	protected String generateLockString(int timeout) {
		final SessionFactoryImplementor factory = getLockable().getFactory();
		final LockOptions lockOptions = new LockOptions( getLockMode() );
		lockOptions.setTimeOut( timeout );
		final SimpleSelect select = new SimpleSelect( factory.getDialect() )
				.setLockOptions( lockOptions )
				.setTableName( getLockable().getRootTableName() )
				.addColumn( getLockable().getRootTableIdentifierColumnNames()[0] )
				.addCondition( getLockable().getRootTableIdentifierColumnNames(), "=?" );
		if ( StringHelper.isNotEmpty( getLockable().getVersionColumnName() ) ) {
			select.addCondition( getLockable().getVersionColumnName(), "=?" );
		}
		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
			select.setComment( getLockMode() + " lock " + getLockable().getEntityName() );
		}
		return select.toStatementString();
	}
}
