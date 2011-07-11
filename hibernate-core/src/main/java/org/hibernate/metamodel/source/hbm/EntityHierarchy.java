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
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLHibernateMapping;

/**
 * Models the source view of an entity hierarchy.
 *
 * @author Steve Ebersole
 */
public class EntityHierarchy extends AbstractSubEntityContainer {
	private final EntitySourceInformation entitySourceInformation;
	private InheritanceType hierarchyInheritanceType = InheritanceType.NO_INHERITANCE;

	public EntityHierarchy(XMLHibernateMapping.XMLClass rootEntity, MappingDocument sourceMappingDocument) {
		this.entitySourceInformation = new EntitySourceInformation( rootEntity, sourceMappingDocument );
	}

	public EntitySourceInformation getEntitySourceInformation() {
		return entitySourceInformation;
	}

	public InheritanceType getHierarchyInheritanceType() {
		return hierarchyInheritanceType;
	}

	@Override
	public void addSubEntityDescriptor(EntityHierarchySubEntity subEntityDescriptor) {
		super.addSubEntityDescriptor( subEntityDescriptor );

		// check inheritance type consistency
		final InheritanceType inheritanceType = Helper.interpretInheritanceType(
				subEntityDescriptor.getEntitySourceInformation().getEntityElement()
		);
		if ( this.hierarchyInheritanceType != InheritanceType.NO_INHERITANCE
				&& this.hierarchyInheritanceType != inheritanceType ) {
			// throw exception
		}
		this.hierarchyInheritanceType = inheritanceType;
	}
}
