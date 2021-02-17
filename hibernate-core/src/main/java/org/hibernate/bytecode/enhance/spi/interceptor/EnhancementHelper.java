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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

/**
 * @author Steve Ebersole
 */
public class EnhancementHelper {

	@FunctionalInterface
	public interface InheritanceChecker {
		boolean hasSubclasses(String entityName);
	}

	/**
	 * Should the given property be included in the owner's base fetch group?
	 */
	public static boolean includeInBaseFetchGroup(
			Property bootMapping,
			boolean isEnhanced,
			InheritanceChecker inheritanceChecker,
			boolean collectionsInDefaultFetchGroupEnabled) {
		final Value value = bootMapping.getValue();

		if ( ! isEnhanced ) {
			if ( value instanceof ToOne ) {
				if ( ( (ToOne) value ).isUnwrapProxy() ) {
					BytecodeInterceptorLogging.MESSAGE_LOGGER.debugf(
							"To-one property `%s#%s` was mapped with LAZY + NO_PROXY but the class was not enhanced",
							bootMapping.getPersistentClass().getEntityName(),
							bootMapping.getName()
					);
				}
			}
			return true;
		}

		// if we get here, we know the property owner is enhanced for laziness
		//
		// NOTE : we make the (potentially untrue) assumption here that
		// if the owner is enhanced, then all classes are enhanced..

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

				BytecodeInterceptorLogging.MESSAGE_LOGGER.lazyGroupIgnoredForToOne(
						bootMapping.getPersistentClass().getEntityName(),
						bootMapping.getName(),
						bootMapping.getLazyGroup()
				);

				// at a later time - for example 6.0 when we can implement the join solution
				// todo (6.0) : implement this
				// return false;

				// for now, fall through
			}

			if ( ! toOne.isReferenceToPrimaryKey() ) {
				// we do not have a reference to the associated primary-key
				return false;
			}

			if ( toOne.getColumnSpan() == 0 ) {
				// generally this would indicate a "shared PK" on-to-one and there
				// is no column for the association on the owner table - do not add
				// the association to the base group (which would force an immediate
				// select from the association table effectively making this
				// association non-lazy)
				return false;
			}

			final boolean unwrapExplicitlyRequested = toOne.isUnwrapProxy() && !toOne.isUnwrapProxyImplicit();

			if ( inheritanceChecker.hasSubclasses( toOne.getReferencedEntityName() ) ) {
				// the associated type has subclasses - we cannot use the enhanced proxy and will generate a HibernateProxy
				if ( unwrapExplicitlyRequested ) {
					// NO_PROXY was explicitly requested
					BytecodeInterceptorLogging.LOGGER.debugf(
							"`%s#%s` was mapped with LAZY and explicit NO_PROXY but the associated entity (`%s`) has subclasses",
							bootMapping.getPersistentClass().getEntityName(),
							bootMapping.getName(),
							toOne.getReferencedEntityName()
					);
				}
				// however, select the fk to create the enhanced-proxy
				return true;
			}

			if ( toOne instanceof ManyToOne && ( (ManyToOne) toOne ).isIgnoreNotFound() ) {
				if ( unwrapExplicitlyRequested ) {
					BytecodeInterceptorLogging.LOGGER.debugf(
							"%s#%s specified NotFoundAction.IGNORE & LazyToOneOption.NO_PROXY; " +
									"skipping FK selection to more efficiently handle NotFoundAction.IGNORE",
							bootMapping.getPersistentClass().getEntityName(),
							bootMapping.getName()
					);
					return false;
				}
			}

			if ( unwrapExplicitlyRequested ) {
				// NO_PROXY was explicitly requested...
				//		- we return true here based on the assumption that `isEnhanced`
				//		is true for all classes - select the FK so we can build the enhanced-proxy
				return true;
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
			BytecodeInterceptorLogging.LOGGER.debug( "Enhancement interception Helper#performWork started temporary Session" );

			isJta = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();

			if ( !isJta ) {
				// Explicitly handle the transactions only if we're not in
				// a JTA environment.  A lazy loading temporary session can
				// be created even if a current session and transaction are
				// open (ex: session.clear() was used).  We must prevent
				// multiple transactions.
				BytecodeInterceptorLogging.LOGGER.debug( "Enhancement interception Helper#performWork starting transaction on temporary Session" );
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
						BytecodeInterceptorLogging.LOGGER.debug( "Enhancement interception Helper#performWork committing transaction on temporary Session" );
						session.getTransaction().commit();
					}
				}
				catch (Exception e) {
					BytecodeInterceptorLogging.LOGGER.warn(
							"Unable to commit JDBC transaction on temporary session used to load lazy " +
									"collection associated to no session"
					);
				}

				// Close the just opened temp Session
				try {
					BytecodeInterceptorLogging.LOGGER.debug( "Enhancement interception Helper#performWork closing temporary Session" );
					session.close();
				}
				catch (Exception e) {
					BytecodeInterceptorLogging.LOGGER.warn( "Unable to close temporary session used to load lazy collection associated to no session" );
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
