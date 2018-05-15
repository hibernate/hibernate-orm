/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.SingularAttribute;

/**
 * Hibernate extension to the JPA {@link SingularAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface SingularPersistentAttribute<O, J> extends PersistentAttributeDescriptor<O, J>, SingularAttribute<O, J> {
	@Override
	default Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	SimpleTypeDescriptor<J> getType();

	@Override
	default SimpleTypeDescriptor<J> getAttributeType() {
		return getType();
	}

	/**
	 * Classifications of the singularity
	 */
	enum SingularAttributeClassification {
		BASIC,
		EMBEDDED,
		ANY,
		ONE_TO_ONE,
		MANY_TO_ONE
	}

	/**
	 * Obtain the classification enum for the attribute.
	 *
	 * @return The classification
	 */
	SingularAttributeClassification getAttributeTypeClassification();

	/**
	 * Describes the "disposition" of the singular attribute.  This is
	 * basically identifying whether the attribute serves as a "normal"
	 * singular attribute or as a special singular attribute such as an
	 * identifier or a version
	 */
	enum Disposition {
		ID,
		VERSION,
		NORMAL
	}

	/**
	 * Returns whether this attribute is <ul>
	 *     <li>part of an id?</li>
	 *     <li>the version attribute?</li>
	 *     <li>or a normal attribute?</li>
	 * </ul>
	 */
	Disposition getDisposition();
}
