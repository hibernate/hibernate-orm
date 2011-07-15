/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.hbm;

import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.binder.RootEntitySource;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements org.hibernate.metamodel.source.binder.EntityHierarchy {
	private final RootEntitySourceImpl rootEntitySource;
	private InheritanceType hierarchyInheritanceType = InheritanceType.NO_INHERITANCE;

	public EntityHierarchyImpl(RootEntitySourceImpl rootEntitySource) {
		this.rootEntitySource = rootEntitySource;
		this.rootEntitySource.injectHierarchy( this );
	}

	@Override
	public InheritanceType getHierarchyInheritanceType() {
		return hierarchyInheritanceType;
	}

	@Override
	public RootEntitySource getRootEntitySource() {
		return rootEntitySource;
	}

	public void processSubclass(SubclassEntitySourceImpl subclassEntitySource) {
		final InheritanceType inheritanceType = Helper.interpretInheritanceType( subclassEntitySource.entityElement() );
		if ( hierarchyInheritanceType == InheritanceType.NO_INHERITANCE ) {
			hierarchyInheritanceType = inheritanceType;
		}
		else if ( hierarchyInheritanceType != inheritanceType ) {
			throw new MappingException( "Mixed inheritance strategies not supported", subclassEntitySource.getOrigin() );
		}
	}
}
