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
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.VersionType;

/**
 * @author Steve Ebersole
 */
public class CacheDataDescriptionImpl implements CacheDataDescription {
	private final boolean mutable;
	private final boolean versioned;
	private final Comparator versionComparator;

	public CacheDataDescriptionImpl(boolean mutable, boolean versioned, Comparator versionComparator) {
		this.mutable = mutable;
		this.versioned = versioned;
		this.versionComparator = versionComparator;
	}

	public boolean isMutable() {
		return mutable;
	}

	public boolean isVersioned() {
		return versioned;
	}

	public Comparator getVersionComparator() {
		return versionComparator;
	}

	public static CacheDataDescriptionImpl decode(PersistentClass model) {
		return new CacheDataDescriptionImpl(
				model.isMutable(),
				model.isVersioned(),
				model.isVersioned() ? ( ( VersionType ) model.getVersion().getType() ).getComparator() : null
		);
	}

	public static CacheDataDescriptionImpl decode(EntityBinding model) {
		return new CacheDataDescriptionImpl(
				model.isMutable(),
				model.isVersioned(),
				getVersionComparator( model )
		);
	}

	public static CacheDataDescriptionImpl decode(Collection model) {
		return new CacheDataDescriptionImpl(
				model.isMutable(),
				model.getOwner().isVersioned(),
				model.getOwner().isVersioned() ? ( ( VersionType ) model.getOwner().getVersion().getType() ).getComparator() : null
		);
	}

	public static CacheDataDescriptionImpl decode(PluralAttributeBinding model) {
		return new CacheDataDescriptionImpl(
				model.isMutable(),
				model.getContainer().seekEntityBinding().isVersioned(),
				getVersionComparator( model.getContainer().seekEntityBinding() )
		);
	}

    public static CacheDataDescriptionImpl decode(EntityPersister persister) {
        return new CacheDataDescriptionImpl(
                !persister.getEntityMetamodel().hasImmutableNaturalId(),
                false,
                null
        );
    }

	private static Comparator getVersionComparator(EntityBinding model ) {
		Comparator versionComparator = null;
		if ( model.isVersioned() ) {
			versionComparator = (
					( VersionType ) model.getHierarchyDetails()
							.getVersioningAttributeBinding()
							.getHibernateTypeDescriptor()
							.getResolvedTypeMapping()
			).getComparator();
		}
		return versionComparator;
	}
}
