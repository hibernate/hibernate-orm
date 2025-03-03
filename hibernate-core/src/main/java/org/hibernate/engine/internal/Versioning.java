/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.VersionJavaType;



import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

/**
 * Utilities for dealing with optimistic locking values.
 *
 * @author Gavin King
 */
public final class Versioning {
	private static final VersionLogger LOG = VersionLogger.INSTANCE;

	/**
	 * Private constructor disallowing instantiation.
	 */
	private Versioning() {
	}

	/**
	 * Create an initial optimistic locking value according the {@link EntityVersionMapping}
	 * contract for the version property.
	 *
	 * @param versionMapping The version mapping
	 * @param session The originating session
	 * @return The initial optimistic locking value
	 */
	public static Object seed(EntityVersionMapping versionMapping, SharedSessionContractImplementor session) {
		final Object seed = versionMapping.getJavaType().seed(
				versionMapping.getLength(),
				versionMapping.getTemporalPrecision() != null
						? versionMapping.getTemporalPrecision()
						: versionMapping.getPrecision(),
				versionMapping.getScale(),
				session
		);
		LOG.seed( seed );
		return seed;
	}


	/**
	 * Create an initial optimistic locking value using the for the version property
	 * <em>if required</em> using the {@link org.hibernate.generator.Generator} contract
	 * and inject it into the snapshot state.
	 *
	 * @param fields The current snapshot state
	 * @param persister The persister of the versioned entity
	 * @param entity The entity instance
	 * @param session The originating session
	 * @return True if we injected a new version value into the fields array; false
	 * otherwise.
	 */
	public static boolean seedVersion(
			Object entity,
			Object[] fields,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		final int versionProperty = persister.getVersionProperty();
		final Object initialVersion = fields[versionProperty];
		if ( isNullInitialVersion( initialVersion ) ) {
			fields[versionProperty] = persister.getVersionGenerator().generate( session, entity, initialVersion, INSERT );
			return true;
		}
		else {
			LOG.initial( initialVersion );
			return false;
		}
	}

	/**
	 * Determines if the value of the assigned  version property should be considered
	 * a "null" value, that is, if it is literally {@code null}, or if it is a negative
	 * integer.
	 *
	 * @param initialVersion The value initially assigned to a version property
	 * @return {@code} if the value should be considered null for this purpose
	 */
	public static boolean isNullInitialVersion(Object initialVersion) {
		return initialVersion == null
			||  // This next bit is to allow for both unsaved-value="negative"
				// and for "older" behavior where version number did not get
				// seeded if it was already set in the object
				// TODO: shift it into unsaved-value strategy
			initialVersion instanceof Number number && number.longValue() < 0;
	}

	/**
	 * Generate the next increment in the optimistic locking value according the
	 * {@link org.hibernate.generator.Generator} contract for the version property.
	 *
	 * @param entity The entity instance
	 * @param currentVersion The current version
	 * @param persister The persister of the versioned entity
	 * @param session The originating session
	 * @return The incremented optimistic locking value.
	 */
	public static Object incrementVersion(
			Object entity,
			Object currentVersion,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		return persister.getVersionGenerator().generate( session, entity, currentVersion, UPDATE );
	}

	/**
	 * Generate the next increment in the optimistic locking value according
	 * the {@link VersionJavaType} contract for the version property.
	 *
	 * @param version The current version
	 * @param versionMapping The version mapping
	 * @param session The originating session
	 * @return The incremented optimistic locking value.
	 */
	public static Object increment(Object version, EntityVersionMapping versionMapping, SharedSessionContractImplementor session) {
		@SuppressWarnings("unchecked")
		final VersionJavaType<Object> versionType = (VersionJavaType<Object>) versionMapping.getJavaType();
		final Object next = versionType.next(
				version,
				versionMapping.getLength(),
				versionMapping.getTemporalPrecision() != null
						? versionMapping.getTemporalPrecision()
						: versionMapping.getPrecision(),
				versionMapping.getScale(),
				session
		);
		LOG.incrementing( version, next );
		return next;
	}

	/**
	 * Inject the optimistic locking value into the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param version The optimistic locking value
	 * @param persister The entity persister
	 */
	public static void setVersion(Object[] fields, Object version, EntityPersister persister) {
		if ( persister.isVersioned() ) {
			fields[ persister.getVersionProperty() ] = version;
		}
	}

	/**
	 * Extract the optimistic locking value out of the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param persister The entity persister
	 * @return The extracted optimistic locking value
	 */
	public static Object getVersion(Object[] fields, EntityPersister persister) {
		return persister.isVersioned() ? fields[persister.getVersionProperty()] : null;
	}

	/**
	 * Do we need to increment the version number, given the dirty properties?
	 *
	 * @param dirtyProperties The array of property indexes which were deemed dirty
	 * @param hasDirtyCollections Were any collections found to be dirty (structurally changed)
	 * @param propertyVersionability An array indicating versionability of each property.
	 * @return True if a version increment is required; false otherwise.
	 */
	public static boolean isVersionIncrementRequired(
			final int[] dirtyProperties,
			final boolean hasDirtyCollections,
			final boolean[] propertyVersionability) {
		if ( hasDirtyCollections ) {
			return true;
		}

		if ( dirtyProperties != null ) {
			for ( int dirtyProperty : dirtyProperties ) {
				if ( propertyVersionability[dirtyProperty] ) {
					return true;
				}
			}
		}

		return false;
	}
}
