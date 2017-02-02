/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			BaselineAttributeInformation attributeInformation,
			VersionValue unsavedValue) {
		super( source, sessionFactory, attributeNumber, attributeName, attributeType, attributeInformation );
		this.unsavedValue = unsavedValue;
	}

	public VersionValue getUnsavedValue() {
		return unsavedValue;
	}
}
