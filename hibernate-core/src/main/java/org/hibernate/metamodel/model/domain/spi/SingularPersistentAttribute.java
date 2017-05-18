/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;

import org.hibernate.metamodel.model.relational.spi.Column;

/**
 * @author Steve Ebersole
 */
public interface SingularPersistentAttribute<O,T>
		extends PersistentAttribute<O,T>, javax.persistence.metamodel.SingularAttribute<O,T> {

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

	/**
	 * todo (6.0) : Again, consider removing this.
	 * 		It is never used by Hibernate code - since we now encapsulate the usage of these
	 * 		as we build TableGroups and Selections/QueryResults/etc.
	 */
	default List<Column> getColumns() {
		return null;
	}

	boolean isNullable();
}
