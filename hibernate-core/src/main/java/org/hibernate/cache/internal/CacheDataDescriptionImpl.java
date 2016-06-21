/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.util.Comparator;

import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;
import org.hibernate.type.descriptor.java.IncomparableComparator;

/**
 * Standard CacheDataDescription implementation.
 *
 * @author Steve Ebersole
 */
public class CacheDataDescriptionImpl implements CacheDataDescription {
	private final boolean mutable;
	private final boolean versioned;
	private final Comparator versionComparator;
	private final Type keyType;

	/**
	 * Constructs a CacheDataDescriptionImpl instance.  Generally speaking, code should use one of the
	 * overloaded {@link #decode} methods rather than direct instantiation.
	 * @param mutable Is the described data mutable?
	 * @param versioned Is the described data versioned?
	 * @param versionComparator The described data's version value comparator (if versioned).
	 * @param keyType
	 */
	public CacheDataDescriptionImpl(boolean mutable, boolean versioned, Comparator versionComparator, Type keyType) {
		this.mutable = mutable;
		this.versioned = versioned;
		this.versionComparator = versionComparator;
		if ( versioned &&
				( versionComparator == null || IncomparableComparator.class.isInstance( versionComparator ) ) ) {
			throw new IllegalArgumentException(
					"versionComparator must not be null or an instance of " + IncomparableComparator.class.getName()
			);
		}
		this.keyType = keyType;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isVersioned() {
		return versioned;
	}

	@Override
	public Comparator getVersionComparator() {
		return versionComparator;
	}

	@Override
	public Type getKeyType() {
		return keyType;
	}

	/**
	 * Builds a CacheDataDescriptionImpl from the mapping model of an entity class.
	 *
	 * @param model The mapping model.
	 *
	 * @return The constructed CacheDataDescriptionImpl
	 */
	public static CacheDataDescriptionImpl decode(PersistentClass model) {
		return new CacheDataDescriptionImpl(
				model.isMutable(),
				model.isVersioned(),
				model.isVersioned()
						? ( (VersionType) model.getVersion().getType() ).getComparator()
						: null,
				model.getIdentifier().getType()
		);
	}

	/**
	 * Builds a CacheDataDescriptionImpl from the mapping model of a collection
	 *
	 * @param model The mapping model.
	 *
	 * @return The constructed CacheDataDescriptionImpl
	 */
	public static CacheDataDescriptionImpl decode(Collection model) {
		return new CacheDataDescriptionImpl(
				model.isMutable(),
				model.getOwner().isVersioned(),
				model.getOwner().isVersioned()
						? ( (VersionType) model.getOwner().getVersion().getType() ).getComparator()
						: null,
				model.getKey().getType()
		);
	}

}
