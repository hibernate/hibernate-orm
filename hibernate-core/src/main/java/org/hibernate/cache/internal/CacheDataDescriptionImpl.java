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
package org.hibernate.cache.internal;

import java.util.Comparator;

import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.type.VersionType;

/**
 * Standard CacheDataDescription implementation.
 *
 * @author Steve Ebersole
 */
public class CacheDataDescriptionImpl implements CacheDataDescription {
	private final boolean mutable;
	private final boolean versioned;
	private final Comparator versionComparator;

	/**
	 * Constructs a CacheDataDescriptionImpl instance.  Generally speaking, code should use one of the
	 * overloaded {@link #decode} methods rather than direct instantiation.
	 *
	 * @param mutable Is the described data mutable?
	 * @param versioned Is the described data versioned?
	 * @param versionComparator The described data's version value comparator (if versioned).
	 */
	public CacheDataDescriptionImpl(boolean mutable, boolean versioned, Comparator versionComparator) {
		this.mutable = mutable;
		this.versioned = versioned;
		this.versionComparator = versionComparator;
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
						: null
		);
	}

	/**
	 * Builds a CacheDataDescriptionImpl from the mapping model of an entity class (using the new metamodel code).
	 *
	 * @param model The mapping model.
	 *
	 * @return The constructed CacheDataDescriptionImpl
	 */
	public static CacheDataDescriptionImpl decode(EntityBinding model) {
		return new CacheDataDescriptionImpl(
				model.getHierarchyDetails().isMutable(),
				model.getHierarchyDetails().isVersioned(),
				getVersionComparator( model )
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
						: null
		);
	}

	/**
	 * Builds a CacheDataDescriptionImpl from the mapping model of a collection (using the new metamodel code).
	 *
	 * @param model The mapping model.
	 *
	 * @return The constructed CacheDataDescriptionImpl
	 */
	public static CacheDataDescriptionImpl decode(PluralAttributeBinding model) {
		return new CacheDataDescriptionImpl(
				model.isMutable(),
				model.getContainer().seekEntityBinding().getHierarchyDetails().isVersioned(),
				getVersionComparator( model.getContainer().seekEntityBinding() )
		);
	}

	private static Comparator getVersionComparator(EntityBinding model ) {
		if ( model.getHierarchyDetails().isVersioned() ) {
			final VersionType versionType = (VersionType) model.getHierarchyDetails()
					.getEntityVersion().getVersioningAttributeBinding()
					.getHibernateTypeDescriptor()
					.getResolvedTypeMapping();

			return versionType.getComparator();
		}

		return null;
	}
}
