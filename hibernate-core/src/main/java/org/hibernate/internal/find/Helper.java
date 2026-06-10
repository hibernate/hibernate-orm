/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FindOption;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import jakarta.persistence.TransactionRequiredException;
import org.hibernate.FindMultipleOption;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.CoercionException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static final FindOption[] NO_OPTIONS = new FindOption[0];

	public static Object coerceId(EntityPersister entityPersister, Object id, SessionFactoryImplementor factory) {
		final var identifierMapping = entityPersister.getIdentifierMapping();
		if ( isLoadByIdComplianceEnabled( factory ) ) {
			final var javaType = identifierMapping.getJavaType();
			if ( !identifierMapping.isVirtual() && !javaType.isInstance( id ) ) {
				// per expectation of EntityHandler#find / EntityHandler#get
				throw new IllegalArgumentException(
						"Given value '%s' did not match expected identifier type '%s' of entity '%s'"
								.formatted( id, javaType.getTypeName(), entityPersister.getEntityName() ) );
			}
			return id;
		}
		else {
			try {
				return identifierMapping.isVirtual()
						? id // special case for a class with an @IdClass
						: identifierMapping.getJavaType().coerce( id );
			}
			catch ( Exception e ) {
				throw new IllegalArgumentException(
						"Argument '%s' could not be converted to the identifier type of entity '%s': %s"
								.formatted( id, entityPersister.getEntityName(), e.getMessage() ),
						e
				);
			}
		}
	}

	public static Object[] coerceIds(EntityPersister entityPersister, List<?> keys, SharedSessionContractImplementor session) {
		Object[] ids = null;
		final int size = keys.size();
		for ( int i = 0; i < size; i++ ) {
			final var factory = session.getFactory();
			final Object key = keys.get( i );
			final Object coerced = coerceId( entityPersister, key, factory );
			if ( coerced != key ) {
				if ( ids == null ) {
					ids = new Object[size];
				}
				ids[i] = coerced;
			}
		}
		return ids == null ? keys.toArray() : ids;
	}

	public static Object coerceNaturalId(EntityPersister entityPersister, Object key) {
		var naturalIdMapping = entityPersister.getNaturalIdMapping();
		assert naturalIdMapping != null;
		try {
			return naturalIdMapping.normalizeInput( key );
		}
		catch (TypeMismatchException | CoercionException e) {
			// per expectation of EntityHandler#find / EntityHandler#get
			var iae = new IllegalArgumentException( e.getMessage() );
			iae.addSuppressed( e );
			throw iae;
		}
	}

	private static boolean isLoadByIdComplianceEnabled(SessionFactoryImplementor factory) {
		return factory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled();
	}

	public static LockOptions makeLockOptions(
			LockMode lockMode,
			PessimisticLockScope lockScope,
			Timeout lockTimeout,
			Locking.FollowOn lockFollowOn) {
		if ( lockMode == null || lockMode == LockMode.NONE ) {
			return LockOptions.NONE;
		}
		if ( lockMode == LockMode.READ ) {
			return LockOptions.READ;
		}

		final var lockOptions = new LockOptions( lockMode );
		lockOptions.setScope( lockScope != null ? lockScope : PessimisticLockScope.NORMAL );
		lockOptions.setTimeout( lockTimeout != null ? lockTimeout : Timeouts.WAIT_FOREVER );
		lockOptions.setFollowOnStrategy( lockFollowOn != null ? lockFollowOn : Locking.FollowOn.ALLOW );
		return lockOptions;
	}

	public static void checkTransactionNeededForLock(
			SharedSessionContractImplementor session,
			LockMode lockMode) {
		if ( lockMode != null
				&& lockMode.greaterThan( LockMode.NONE )
				&& !session.isTransactionInProgress() ) {
			throw new TransactionRequiredException( "Transaction required for lock mode " + lockMode );
		}
	}

	public static <T> void verifyGetMultipleResults(
			List<T> results,
			String entityName,
			List<?> keys,
			FindOption... findOptions) {
		// how detailed we can get with the error message depends on whether
		// results are ordered or unordered, defined by the OrderingMode option
		final var orderingMode = determineOrderingMode( findOptions );

		if ( orderingMode == FindMultipleOption.OrderingMode.UNORDERED ) {
			for ( int i = 0; i < results.size(); i++ ) {
				if ( results.get( i ) == null ) {
					throw new EntityNotFoundException(
							"No entity of type '%s' existed for one or more of the given ids"
									.formatted( entityName )
					);
				}
			}
		}
		else {
			List<Object> missingKeys = null;
			for ( int i = 0; i < results.size(); i++ ) {
				if ( results.get( i ) == null ) {
					if ( missingKeys == null ) {
						missingKeys = new ArrayList<>();
					}
					missingKeys.add( keys.get( i ) );
				}
			}
			if ( missingKeys != null ) {
				throw new EntityNotFoundException(
						"No entity of type '%s' existed for the ids %s"
								.formatted( entityName, missingKeys )
				);
			}
		}
	}

	private static FindMultipleOption.OrderingMode determineOrderingMode(FindOption[] findOptions) {
		if ( findOptions != null ) {
			for ( var findOption : findOptions ) {
				if ( findOption instanceof FindMultipleOption.OrderingMode requestedMode ) {
					return requestedMode;
				}
			}
		}
		return FindMultipleOption.OrderingMode.ORDERED;
	}
}
