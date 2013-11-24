/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.VersionType;

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
	 * Create an initial optimistic locking value according the {@link VersionType}
	 * contract for the version property.
	 *
	 * @param versionType The version type.
	 * @param session The originating session
	 * @return The initial optimistic locking value
	 */
	private static Object seed(VersionType versionType, SessionImplementor session) {
		final Object seed = versionType.seed( session );
		LOG.tracef( "Seeding: %s", seed );
		return seed;
	}

	/**
	 * Create an initial optimistic locking value according the {@link VersionType}
	 * contract for the version property <b>if required</b> and inject it into
	 * the snapshot state.
	 *
	 * @param fields The current snapshot state
	 * @param versionProperty The index of the version property
	 * @param versionType The version type
	 * @param session The originating session
	 * @return True if we injected a new version value into the fields array; false
	 * otherwise.
	 */
	public static boolean seedVersion(
			Object[] fields,
			int versionProperty,
			VersionType versionType,
			SessionImplementor session) {
		final Object initialVersion = fields[versionProperty];
		if (
			initialVersion==null ||
			// This next bit is to allow for both unsaved-value="negative"
			// and for "older" behavior where version number did not get
			// seeded if it was already set in the object
			// TODO: shift it into unsaved-value strategy
			( (initialVersion instanceof Number) && ( (Number) initialVersion ).longValue()<0 )
		) {
			fields[versionProperty] = seed( versionType, session );
			return true;
		}
		LOG.tracev( "Using initial version: {0}", initialVersion );
		return false;
	}


	/**
	 * Generate the next increment in the optimistic locking value according
	 * the {@link VersionType} contract for the version property.
	 *
	 * @param version The current version
	 * @param versionType The version type
	 * @param session The originating session
	 * @return The incremented optimistic locking value.
	 */
	@SuppressWarnings("unchecked")
	public static Object increment(Object version, VersionType versionType, SessionImplementor session) {
		final Object next = versionType.next( version, session );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Incrementing: %s to %s",
					versionType.toLoggableString( version, session.getFactory() ),
					versionType.toLoggableString( next, session.getFactory() )
			);
		}
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
		if ( !persister.isVersioned() ) {
			return;
		}
		fields[ persister.getVersionProperty() ] = version;
	}

	/**
	 * Extract the optimistic locking value out of the entity state snapshot.
	 *
	 * @param fields The state snapshot
	 * @param persister The entity persister
	 * @return The extracted optimistic locking value
	 */
	public static Object getVersion(Object[] fields, EntityPersister persister) {
		if ( !persister.isVersioned() ) {
			return null;
		}
		return fields[ persister.getVersionProperty() ];
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
