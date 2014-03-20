/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.MappingException;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public enum InheritanceType {
	NONE,
	JOINED,
	SINGLE,
	TABLE_PER_CLASS;

	/**
	 * @param entityBinding The class for which to get the inheritance type.
	 *
	 * @return The inheritance type of this class. NONE, if this class does not inherit from
	 *         another persistent class.
	 */
	public static InheritanceType get(EntityBinding entityBinding) {
		final EntityBinding superEntityBinding = entityBinding.getSuperEntityBinding();
		if ( superEntityBinding == null ) {
			return InheritanceType.NONE;
		}

		// We assume that every subclass is of the same type.
		final EntityBinding subEntityBinding = superEntityBinding.getDirectSubEntityBindings().get( 0 );

		if ( subEntityBinding.getHierarchyDetails().getInheritanceType() == org.hibernate.metamodel.spi.binding.InheritanceType.SINGLE_TABLE ) {
			return InheritanceType.SINGLE;
		}
		else if ( subEntityBinding.getHierarchyDetails().getInheritanceType() ==  org.hibernate.metamodel.spi.binding.InheritanceType.JOINED ) {
			return InheritanceType.JOINED;
		}
		else if ( subEntityBinding.getHierarchyDetails().getInheritanceType() ==  org.hibernate.metamodel.spi.binding.InheritanceType.TABLE_PER_CLASS ) {
			return InheritanceType.TABLE_PER_CLASS;
		}

		throw new MappingException( "Unknown subclass class: " + subEntityBinding.getClass() );
	}
}
