/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.metamodel.source.internal.annotations.entity.MappedSuperclassTypeMetadata;
import org.hibernate.metamodel.source.spi.MappedSuperclassSource;

/**
 * Adapter for a MappedSuperclass
 *
 * @author Steve Ebersole
 */
public class MappedSuperclassSourceImpl extends IdentifiableTypeSourceAdapter implements MappedSuperclassSource {
	/**
	 * Form for use in creating MappedSuperclassSource that are the supers of the root entity
	 *
	 * @param mappedSuperclassTypeMetadata Metadata about the MappedSuperclass
	 * @param hierarchy The hierarchy
	 */
	protected MappedSuperclassSourceImpl(
			MappedSuperclassTypeMetadata mappedSuperclassTypeMetadata,
			EntityHierarchySourceImpl hierarchy) {
		// false here indicates that this is not the root entity of a hierarchy
		super( mappedSuperclassTypeMetadata, hierarchy, false );
	}

	/**
	 * Form for use in creating MappedSuperclassSource that are part of the subclass tree of the root entity
	 *
	 * @param mappedSuperclassTypeMetadata Metadata about the MappedSuperclass
	 * @param hierarchy The hierarchy
	 * @param superTypeSource The source object for the super type.
	 */
	protected MappedSuperclassSourceImpl(
			MappedSuperclassTypeMetadata mappedSuperclassTypeMetadata,
			EntityHierarchySourceImpl hierarchy,
			IdentifiableTypeSourceAdapter superTypeSource) {
		super( mappedSuperclassTypeMetadata, hierarchy, superTypeSource );
	}
}
