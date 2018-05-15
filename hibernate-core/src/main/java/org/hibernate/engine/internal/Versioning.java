/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;

import org.jboss.logging.Logger;

/**
 * Utilities for dealing with optimistic locking values.
 *
 * @author Gavin King
 */
public final class Versioning {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			Versioning.class.getName()
	);

	/**
	 * Private constructor disallowing instantiation.
	 */
	private Versioning() {
	}

	/**
	 * Create an initial optimistic locking value according the {@link VersionSupport}
	 * contract for the version property.
	 *
	 * @param versionType The version type.
	 * @param session The originating session
	 * @return The initial optimistic locking value
	 */
	private static Object seed(VersionSupport versionType, SharedSessionContractImplementor session) {
		final Object seed = versionType.seed( session );
		LOG.tracef( "Seeding: %s", seed );
		return seed;
	}

	/**
	 * Create an initial optimistic locking value according the {@link VersionSupport}
	 * contract for the version property <b>if required</b> and inject it into
	 * the snapshot state.
	 *
	 * @return True if we injected a new version value into the fields array; false
	 * otherwise.
	 */
	public static boolean seedVersion(
			Object[] fields,
			VersionDescriptor versionDescriptor,
			SharedSessionContractImplementor session) {

		final int versionPosition = versionDescriptor.getStateArrayPosition();
		final Object initialVersion = fields[ versionPosition ];
		if (
			initialVersion==null ||
			// This next bit is to allow for both unsaved-value="negative"
			// and for "older" behavior where version number did not get
			// seeded if it was already set in the object
			// TODO: shift it into unsaved-value strategy
			( (initialVersion instanceof Number) && ( (Number) initialVersion ).longValue()<0 )
		) {
			fields[versionPosition] = seed( versionDescriptor.getVersionSupport(), session );
			return true;
		}
		LOG.tracev( "Using initial version: {0}", initialVersion );
		return false;
	}


	/**
	 * Generate the next increment in the optimistic locking value according
	 * the {@link VersionSupport} contract for the version property.
	 *
	 * @param version The current version
	 * @param versionType The version type
	 * @param session The originating session
	 * @return The incremented optimistic locking value.
	 */
	@SuppressWarnings("unchecked")
	public static Object increment(Object version, VersionSupport versionType, SharedSessionContractImplementor session) {
		final Object next = versionType.next( version, session );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Incrementing: %s to %s",
					versionType.toLoggableString( version ),
					versionType.toLoggableString( next )
			);
		}
		return next;
	}

	/**
	 * Inject the optimistic locking value into the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param version The optimistic locking value
	 * @param descriptor The entity descriptor
	 */
	public static void setVersion(Object[] fields, Object version, EntityTypeDescriptor descriptor) {
		final VersionDescriptor<Object, Object> versionDescriptor = descriptor.getHierarchy().getVersionDescriptor();
		if ( versionDescriptor == null ) {
			return;
		}

		final int versionPosition = versionDescriptor.getStateArrayPosition();

		fields[ versionPosition ] = version;
	}

	/**
	 * Extract the optimistic locking value out of the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param descriptor The entity descriptor
	 * @return The extracted optimistic locking value
	 */
	public static Object getVersion(Object[] fields, EntityTypeDescriptor descriptor) {
		final VersionDescriptor<Object, Object> versionDescriptor = descriptor.getHierarchy().getVersionDescriptor();
		if ( versionDescriptor == null ) {
			return null;
		}

		final int versionPosition = versionDescriptor.getStateArrayPosition();

		return fields[ versionPosition ];
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
		for ( int dirtyProperty : dirtyProperties ) {
			if ( propertyVersionability[dirtyProperty] ) {
				return true;
			}
		}
		return false;
	}
}
