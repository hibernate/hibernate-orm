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
package org.hibernate.tuple.entity;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.AbstractNonIdentifierAttribute;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.Type;

/**
 * Represents a version property within the Hibernate runtime-metamodel.
 *
 * @author Steve Ebersole
 */
public class VersionProperty extends AbstractNonIdentifierAttribute {

    private final VersionValue unsavedValue;

	/**
	 * Constructs VersionProperty instances.
	 *
	 * @param source Reference back to the source of this attribute (the persister)
	 * @param sessionFactory The session factory this is part of.
	 * @param attributeNumber The attribute number within thje
	 * @param attributeName The name by which the property can be referenced within
	 * its owner.
	 * @param attributeType The Hibernate Type of this property.
	 * @param attributeInformation The basic attribute information.
	 * @param unsavedValue The value which, if found as the value of
	 * this (i.e., the version) property, represents new (i.e., un-saved)
	 * instances of the owning entity.
	 */
	public VersionProperty(
			EntityPersister source,
			SessionFactoryImplementor sessionFactory,
			int attributeNumber,
			String attributeName,
			Type attributeType,
			BaselineAttributeInformation attributeInformation, VersionValue unsavedValue) {
		super( source, sessionFactory, attributeNumber, attributeName, attributeType, attributeInformation );
		this.unsavedValue = unsavedValue;
	}

    public VersionValue getUnsavedValue() {
        return unsavedValue;
    }
}
