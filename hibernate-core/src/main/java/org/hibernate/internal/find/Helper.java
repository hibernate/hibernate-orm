/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FindOption;
import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.OrderingMode;
import org.hibernate.Timeouts;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static final FindOption[] NO_OPTIONS = new FindOption[0];

	public static Object coerceId(EntityPersister entityPersister, Object id, SessionFactoryImplementor factory) {
		if ( isLoadByIdComplianceEnabled( factory ) ) {
			final var identifierMapping = entityPersister.getIdentifierMapping();
			if ( !identifierMapping.getJavaType().isInstance( id ) ) {
				// per expectation of EntityHandler#find / EntityHandler#get
				throw new IllegalArgumentException( String.format( Locale.ROOT,
						"Given value (%s) did not match expected identifier type for entity (%s) : %s",
						id,
						entityPersister.getEntityName(),
						identifierMapping.getJavaType().getTypeName()
				) );
			}
			return id;
		}
		else {
			try {
				final var identifierMapping = entityPersister.getIdentifierMapping();
				return identifierMapping.isVirtual()
						? id // special case for a class with an @IdClass
						: identifierMapping.getJavaType().coerce( id );
			}
			catch ( Exception e ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Argument `%s` could not be converted to the identifier type of entity `%s` : %s",
								id,
								entityPersister.getEntityName(),
								e.getMessage()
						),
						e
				);
			}
		}
	}

	public static Object coerceNaturalId(EntityPersister entityPersister, Object key) {
		var naturalIdMapping = entityPersister.getNaturalIdMapping();
		assert naturalIdMapping != null;
		try {
			return naturalIdMapping.normalizeInput( key );
		}
		catch (TypeMismatchException e) {
			// per expectation of EntityHandler#find / EntityHandler#get
			var iae = new IllegalArgumentException( e.getMessage() );
			iae.addSuppressed( e );
			throw iae;
		}
	}

	private static boolean isLoadByIdComplianceEnabled(SessionFactoryImplementor factory) {
		return factory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled();
	}

	public static LockOptions makeLockOptions(LockMode lockMode, Locking.Scope lockScope, Timeout lockTimeout, Locking.FollowOn lockFollowOn) {
		if ( lockMode == null || lockMode == LockMode.NONE ) {
			return LockOptions.NONE;
		}
		if ( lockMode == LockMode.READ ) {
			return LockOptions.READ;
		}

		final var lockOptions = new LockOptions( lockMode );
		lockOptions.setScope( lockScope != null ? lockScope : Locking.Scope.ROOT_ONLY );
		lockOptions.setTimeout( lockTimeout != null ? lockTimeout : Timeouts.WAIT_FOREVER );
		lockOptions.setFollowOnStrategy( lockFollowOn != null ? lockFollowOn : Locking.FollowOn.ALLOW );
		return lockOptions;
	}

	public static <T> void verifyGetMultipleResults(
			List<T> results,
			String entityName,
			List<?> keys,
			FindOption... findOptions) {
		// how detailed we can get with the error message depends on whether
		// results are ordered or unordered, defined by the OrderingMode option
		final OrderingMode orderingMode = determineOrderingMode( findOptions );

		if ( orderingMode == OrderingMode.UNORDERED ) {
			for ( int i = 0; i < results.size(); i++ ) {
				if ( results.get( i ) == null ) {
					throw new EntityNotFoundException(
							String.format(
									Locale.ROOT,
									"One or more ids could not be found - `%s`",
									entityName
							)
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
						String.format(
								Locale.ROOT,
								"No entity of type `%s` existed for keys `%s`",
								entityName,
								missingKeys
						)
				);
			}
		}
	}

	private static OrderingMode determineOrderingMode(FindOption[] findOptions) {
		var mode = OrderingMode.ORDERED;
		if ( findOptions != null ) {
			for ( FindOption findOption : findOptions ) {
				if ( findOption instanceof OrderingMode requestedMode ) {
					mode = requestedMode;
					break;
				}
			}
		}
		return mode;
	}
}
