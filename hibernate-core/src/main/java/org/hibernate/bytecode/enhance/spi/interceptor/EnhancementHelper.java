/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Locale;
import java.util.function.BiFunction;

import org.hibernate.FlushMode;
import org.hibernate.LazyInitializationException;
import org.hibernate.bytecode.BytecodeLogging;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

/**
 * @author Steve Ebersole
 */
public class EnhancementHelper {
	/**
	 * Should the given property be included in the owner's base fetch group?
	 */
	public static boolean includeInBaseFetchGroup(
			Property bootMapping,
			boolean isEnhanced,
			boolean collectionsInDefaultFetchGroupEnabled) {
		final Value value = bootMapping.getValue();

		if ( ! isEnhanced ) {
			if ( value instanceof ToOne ) {
				if ( ( (ToOne) value ).isUnwrapProxy() ) {
					BytecodeLogging.MESSAGE_LOGGER.debugf(
							"To-one property `%s#%s` was mapped with LAZY + NO_PROXY but the class was not enhanced",
							bootMapping.getPersistentClass().getEntityName(),
							bootMapping.getName()
					);
				}
			}
			return true;
		}

		// we know the property owner is enhanced for laziness...

		if ( value instanceof ToOne ) {
			final ToOne toOne = (ToOne) value;

			if ( ! toOne.isLazy() ) {
				// its not lazy... select it
				return true;
			}

			// it is lazy.  see if we should select the FK

			if ( bootMapping.getLazyGroup() != null ) {
				// a non-base fetch group was explicitly specified
				//
				// really this should indicate to not select it as part of the base group.
				// however, at the time being that leads to inefficient SQL - so for now
				// we simply log a message that we are ignoring the `@LazyGroup` for to-ones

				BytecodeLogging.MESSAGE_LOGGER.lazyGroupIgnoredForToOne(
						bootMapping.getPersistentClass().getEntityName(),
						bootMapping.getName(),
						bootMapping.getLazyGroup()
				);

				// at a later time - for example 6.0 when we can implement the join solution
				// return false;

				// for now, fall through
			}

			if ( ! toOne.isReferenceToPrimaryKey() ) {
				// we do not have a reference to the associated primary-key
				return false;
			}

			final boolean unwrapExplicitlyRequested = toOne.isUnwrapProxy() && !toOne.isUnwrapProxyImplicit();

			if ( unwrapExplicitlyRequested ) {
				// NO_PROXY was explicitly requested
				return false;
			}



			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// original

			if ( toOne.isLazy() ) {
				if ( toOne.isUnwrapProxy() ) {
					if ( toOne instanceof OneToOne ) {
						return false;
					}
					// include it in the base fetch group so long as the config allows
					// using the FK to create an "enhancement proxy"
					//return allowEnhancementAsProxy;
					// ^^ previously we had to explicitly enable use of enhanced proxies.
					//		for the moment just return `true` assuming we can.
					//
					//		there are cases where this block overall misses quite a few cases
					//		where it returns the least optimal value.  This is true even outside
					//		this enhanced-proxy point
					//
					//		those will all be addressed in the commits for HHH-13658
					return true;
				}

			}

			return true;
		}

		return collectionsInDefaultFetchGroupEnabled && ( value instanceof Collection )
				|| ! bootMapping.isLazy();
	}

	public static <T> T performWork(
			BytecodeLazyAttributeInterceptor interceptor,
			BiFunction<SharedSessionContractImplementor, Boolean, T> work,
			String entityName,
			String attributeName) {
		SharedSessionContractImplementor session = interceptor.getLinkedSession();

		boolean isTempSession = false;
		boolean isJta = false;

		// first figure out which Session to use
		if ( session == null ) {
			if ( interceptor.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( interceptor, entityName, attributeName );
				isTempSession = true;
			}
			else {
				throwLazyInitializationException( Cause.NO_SESSION, entityName, attributeName );
			}
		}
		else if ( !session.isOpen() ) {
			if ( interceptor.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( interceptor, entityName, attributeName );
				isTempSession = true;
			}
			else {
				throwLazyInitializationException( Cause.CLOSED_SESSION, entityName, attributeName );
			}
		}
		else if ( !session.isConnected() ) {
			if ( interceptor.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( interceptor, entityName, attributeName );
				isTempSession = true;
			}
			else {
				throwLazyInitializationException( Cause.DISCONNECTED_SESSION, entityName, attributeName);
			}
		}

		// If we are using a temporary Session, begin a transaction if necessary
		if ( isTempSession ) {
			BytecodeLogging.LOGGER.debug( "Enhancement interception Helper#performWork started temporary Session" );

			isJta = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();

			if ( !isJta ) {
				// Explicitly handle the transactions only if we're not in
				// a JTA environment.  A lazy loading temporary session can
				// be created even if a current session and transaction are
				// open (ex: session.clear() was used).  We must prevent
				// multiple transactions.
				BytecodeLogging.LOGGER.debug( "Enhancement interception Helper#performWork starting transaction on temporary Session" );
				session.beginTransaction();
			}
		}

		try {
			// do the actual work
			return work.apply( session, isTempSession );
		}
		finally {
			if ( isTempSession ) {
				try {
					// Commit the JDBC transaction if we started one.
					if ( !isJta ) {
						BytecodeLogging.LOGGER.debug( "Enhancement interception Helper#performWork committing transaction on temporary Session" );
						session.getTransaction().commit();
					}
				}
				catch (Exception e) {
					BytecodeLogging.LOGGER.warn(
							"Unable to commit JDBC transaction on temporary session used to load lazy " +
									"collection associated to no session"
					);
				}

				// Close the just opened temp Session
				try {
					BytecodeLogging.LOGGER.debug( "Enhancement interception Helper#performWork closing temporary Session" );
					session.close();
				}
				catch (Exception e) {
					BytecodeLogging.LOGGER.warn( "Unable to close temporary session used to load lazy collection associated to no session" );
				}
			}
		}
	}

	enum Cause {
		NO_SESSION,
		CLOSED_SESSION,
		DISCONNECTED_SESSION,
		NO_SF_UUID
	}

	private static void throwLazyInitializationException(Cause cause, String entityName, String attributeName) {
		final String reason;
		switch ( cause ) {
			case NO_SESSION: {
				reason = "no session and settings disallow loading outside the Session";
				break;
			}
			case CLOSED_SESSION: {
				reason = "session is closed and settings disallow loading outside the Session";
				break;
			}
			case DISCONNECTED_SESSION: {
				reason = "session is disconnected and settings disallow loading outside the Session";
				break;
			}
			case NO_SF_UUID: {
				reason = "could not determine SessionFactory UUId to create temporary Session for loading";
				break;
			}
			default: {
				reason = "<should never get here>";
			}
		}

		final String message = String.format(
				Locale.ROOT,
				"Unable to perform requested lazy initialization [%s.%s] - %s",
				entityName,
				attributeName,
				reason
		);

		throw new LazyInitializationException( message );
	}

	private static SharedSessionContractImplementor openTemporarySessionForLoading(
			BytecodeLazyAttributeInterceptor interceptor,
			String entityName,
			String attributeName) {
		if ( interceptor.getSessionFactoryUuid() == null ) {
			throwLazyInitializationException( Cause.NO_SF_UUID, entityName, attributeName );
		}

		final SessionFactoryImplementor sf = (SessionFactoryImplementor)
				SessionFactoryRegistry.INSTANCE.getSessionFactory( interceptor.getSessionFactoryUuid() );
		final SharedSessionContractImplementor session = (SharedSessionContractImplementor) sf.openSession();
		session.getPersistenceContextInternal().setDefaultReadOnly( true );
		session.setHibernateFlushMode( FlushMode.MANUAL );
		return session;
	}
}
