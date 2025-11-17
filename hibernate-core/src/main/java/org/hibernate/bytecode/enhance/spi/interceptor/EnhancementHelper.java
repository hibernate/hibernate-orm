/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Locale;
import java.util.function.BiFunction;

import org.hibernate.FlushMode;
import org.hibernate.LazyInitializationException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;

import static org.hibernate.bytecode.enhance.spi.interceptor.BytecodeInterceptorLogging.BYTECODE_INTERCEPTOR_LOGGER;

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
		final var value = bootMapping.getValue();

		if ( ! isEnhanced ) {
			if ( value instanceof ToOne toOne ) {
				if ( toOne.isUnwrapProxy() ) {
					BYTECODE_INTERCEPTOR_LOGGER.toOneLazyNoProxyButNotEnhanced(
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

		if ( value instanceof ToOne toOne ) {

			if ( ! toOne.isLazy() ) {
				// it's not lazy... select it
				return true;
			}

			// it is lazy.  see if we should select the FK

			if ( bootMapping.getLazyGroup() != null ) {
				// a non-base fetch group was explicitly specified
				//
				// really this should indicate to not select it as part of the base group.
				// however, at the time being that leads to inefficient SQL - so for now
				// we simply log a message that we are ignoring the `@LazyGroup` for to-ones

				BYTECODE_INTERCEPTOR_LOGGER.lazyGroupIgnoredForToOne(
						bootMapping.getLazyGroup(),
						bootMapping.getPersistentClass().getEntityName(),
						bootMapping.getName()
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
					BYTECODE_INTERCEPTOR_LOGGER.lazyNoProxyButAssociatedHasSubclasses(
							bootMapping.getPersistentClass().getEntityName(),
							bootMapping.getName(),
							toOne.getReferencedEntityName()
					);
				}
				// however, select the fk to create the enhanced-proxy
				return true;
			}

			if ( toOne instanceof ManyToOne manyToOne && manyToOne.isIgnoreNotFound() ) {
				if ( unwrapExplicitlyRequested ) {
					BYTECODE_INTERCEPTOR_LOGGER.notFoundIgnoreWithNoProxySkippingFkSelection(
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

		final boolean isTempSession;
		final boolean isJta;

		// first figure out which Session to use
		if ( session == null ) {
			if ( interceptor.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( interceptor, entityName, attributeName );
				isTempSession = true;
			}
			else {
				throw createLazyInitializationException( Cause.NO_SESSION, entityName, attributeName );
			}
		}
		else if ( !session.isOpen() ) {
			if ( interceptor.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( interceptor, entityName, attributeName );
				isTempSession = true;
			}
			else {
				throw createLazyInitializationException( Cause.CLOSED_SESSION, entityName, attributeName );
			}
		}
		else if ( !session.isConnected() ) {
			if ( interceptor.allowLoadOutsideTransaction() ) {
				session = openTemporarySessionForLoading( interceptor, entityName, attributeName );
				isTempSession = true;
			}
			else {
				throw createLazyInitializationException( Cause.DISCONNECTED_SESSION, entityName, attributeName);
			}
		}
		else {
			isTempSession = false;
		}

		// If we are using a temporary Session, begin a transaction if necessary
		if ( isTempSession ) {
			BYTECODE_INTERCEPTOR_LOGGER.enhancementHelperStartedTemporarySession();

			isJta = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();

			if ( !isJta ) {
				// Explicitly handle the transactions only if we're not in
				// a JTA environment.  A lazy loading temporary session can
				// be created even if a current session and transaction are
				// open (ex: session.clear() was used).  We must prevent
				// multiple transactions.
				BYTECODE_INTERCEPTOR_LOGGER.enhancementHelperStartingTransactionOnTemporarySession();
				session.beginTransaction();
			}
		}
		else {
			isJta = false;
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
						BYTECODE_INTERCEPTOR_LOGGER.enhancementHelperCommittingTransactionOnTemporarySession();
						session.getTransaction().commit();
					}
				}
				catch (Exception e) {
					BYTECODE_INTERCEPTOR_LOGGER.unableToCommitTransactionOnTemporarySession();
				}

				// Close the just opened temp Session
				try {
					BYTECODE_INTERCEPTOR_LOGGER.enhancementHelperClosingTemporarySession();
					session.close();
				}
				catch (Exception e) {
					BYTECODE_INTERCEPTOR_LOGGER.unableToCloseTemporarySession();
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

	private static LazyInitializationException createLazyInitializationException(final Cause cause, final String entityName, final String attributeName) {
		final String reason = switch ( cause ) {
			case NO_SESSION -> "no session and settings disallow loading outside the Session";
			case CLOSED_SESSION -> "session is closed and settings disallow loading outside the Session";
			case DISCONNECTED_SESSION -> "session is disconnected and settings disallow loading outside the Session";
			case NO_SF_UUID -> "could not determine SessionFactory UUId to create temporary Session for loading";
		};

		final String message = String.format(
				Locale.ROOT,
				"Unable to perform requested lazy initialization [%s.%s] - %s",
				entityName,
				attributeName,
				reason
		);

		return new LazyInitializationException( message );
	}

	private static SharedSessionContractImplementor openTemporarySessionForLoading(
			BytecodeLazyAttributeInterceptor interceptor,
			String entityName,
			String attributeName) {
		if ( interceptor.getSessionFactoryUuid() == null ) {
			throw createLazyInitializationException( Cause.NO_SF_UUID, entityName, attributeName );
		}

		final var factory = SessionFactoryRegistry.INSTANCE.getSessionFactory( interceptor.getSessionFactoryUuid() );
		final var session = factory.openSession();
		session.getPersistenceContextInternal().setDefaultReadOnly( true );
		session.setHibernateFlushMode( FlushMode.MANUAL );
		return session;
	}
}
